# syntax=docker/dockerfile:1

# Build stage: compile and package with the Maven wrapper on a JDK 25 image.
# Tests are not run here; CI runs them before building the image (.github/workflows/backend-image.yml).
# It runs on the platform of the machine that builds ($BUILDPLATFORM): the jar does not depend on
# the CPU, so a multi-platform build (linux/amd64 and linux/arm64, e.g. a t4g Graviton instance)
# compiles once natively and only the runtime stage below is assembled per target platform.
# BuildKit sets BUILDPLATFORM; the default only serves the classic builder, which does not.
ARG BUILDPLATFORM=linux/amd64
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependencies first, so they stay cached while only the sources change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests package \
    && cp target/*.jar app.jar \
    && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# Runtime stage: JRE 25 only, running as a non-root user.
FROM eclipse-temurin:25-jre
RUN groupadd --system quadrapp && useradd --system --gid quadrapp --no-create-home --shell /usr/sbin/nologin quadrapp
WORKDIR /app

# Spring Boot layers, from the least to the most frequently changed.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER quadrapp
EXPOSE 8080

# The heap follows the container memory limit. Override JAVA_TOOL_OPTIONS to change it.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

# There is no Spring Boot Actuator in the project, and the JRE image has no curl or wget.
# The check sends a plain HTTP request with bash to /v3/api-docs (public, served by springdoc)
# and expects a 200 status line.
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=4 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /v3/api-docs HTTP/1.1\\r\\nHost: localhost\\r\\nConnection: close\\r\\n\\r\\n' >&3 && head -n 1 <&3 | grep -q ' 200'"]

ENTRYPOINT ["java", "-jar", "app.jar"]
