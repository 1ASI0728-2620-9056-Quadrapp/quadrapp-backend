package com.upc.quadrapp.architecture;

import com.upc.quadrapp.QuadrappBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails when a bounded context uses another module's internal types instead of its
 * published interfaces, or when modules depend on each other in a cycle (DD-01).
 */
class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(QuadrappBackendApplication.class);

    @Test
    void modulesOnlyUsePublishedInterfacesOfOtherModules() {
        MODULES.verify();
    }
}
