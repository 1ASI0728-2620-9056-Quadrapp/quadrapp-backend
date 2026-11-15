package com.upc.quadrapp;

import com.upc.quadrapp.support.QuadrappIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * The application starts on an empty database: Flyway creates every module schema and
 * Hibernate validates the JPA mappings against it.
 */
@QuadrappIntegrationTest
class QuadrappBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
