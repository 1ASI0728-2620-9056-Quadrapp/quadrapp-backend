package com.upc.quadrapp.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Boots the whole application against an empty PostgreSQL container.
 * <p>
 * The {@code dev} profile is active because the in-memory OTP store and the sensing
 * adapters only exist in that profile for now. The JWT secret and the hashing pepper are
 * fixed, non-secret test values.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "quadrapp.security.jwt.secret-base64="
                + "cXVhZHJhcHAtaW50ZWdyYXRpb24tdGVzdC1qd3Qtc2lnbmluZy1rZXktMDEyMzQ1Njc4OQ==",
        "quadrapp.security.hash-pepper-base64="
                + "cXVhZHJhcHAtaW50ZWdyYXRpb24tdGVzdC1oYXNoLXBlcHBlci0wMTIzNDU2Nzg5"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("dev")
public @interface QuadrappIntegrationTest {
}
