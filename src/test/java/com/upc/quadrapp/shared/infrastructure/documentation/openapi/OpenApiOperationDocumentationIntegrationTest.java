package com.upc.quadrapp.shared.infrastructure.documentation.openapi;

import com.jayway.jsonpath.JsonPath;
import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every REST operation published in {@code /v3/api-docs} has a readable summary and an explicit
 * operationId, so Swagger UI never shows generated names such as {@code list_4} or {@code getById_2}.
 */
@QuadrappIntegrationTest
@AutoConfigureMockMvc
class OpenApiOperationDocumentationIntegrationTest {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");
    private static final Pattern GENERATED_SUFFIX = Pattern.compile(".*_\\d+$");

    @Autowired
    private MockMvc mvc;

    @Test
    @SuppressWarnings("unchecked")
    void everyOperationHasASummaryAndAUniqueExplicitOperationId() throws Exception {
        String json = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Map<String, Object>> paths = JsonPath.read(json, "$.paths");
        List<String> problems = new ArrayList<>();
        Map<String, String> owners = new HashMap<>();
        int operations = 0;

        for (var path : paths.entrySet()) {
            for (var entry : path.getValue().entrySet()) {
                if (!HTTP_METHODS.contains(entry.getKey())) {
                    continue;
                }
                operations++;
                String where = entry.getKey().toUpperCase() + " " + path.getKey();
                Map<String, Object> operation = (Map<String, Object>) entry.getValue();
                Object summary = operation.get("summary");
                Object operationId = operation.get("operationId");

                if (!(summary instanceof String s) || s.isBlank()) {
                    problems.add(where + " has no summary");
                }
                if (!(operationId instanceof String id) || id.isBlank()) {
                    problems.add(where + " has no operationId");
                    continue;
                }
                if (GENERATED_SUFFIX.matcher(id).matches()) {
                    problems.add(where + " has a generated operationId: " + id);
                }
                String previous = owners.putIfAbsent(id, where);
                if (previous != null) {
                    problems.add("operationId " + id + " is used by " + previous + " and " + where);
                }
            }
        }

        assertThat(operations).as("documented operations").isPositive();
        assertThat(problems).as("operations without readable documentation").isEmpty();
    }
}
