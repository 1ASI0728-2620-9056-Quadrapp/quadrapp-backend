package com.upc.quadrapp.acceptance.parkingconfiguration;

import com.upc.quadrapp.acceptance.AcceptanceSteps;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US17: the administrator configures the zones and spaces of a parking lot. A space belongs to a
 * level of its zone, so registering a space in a zone goes through one of its levels.
 */
public class ConfigureZonesAndSpacesSteps extends AcceptanceSteps {

    private static final String SPACE_LABEL = "A-01";

    private long adminId;
    private long lotId;
    private long zoneId;
    private long levelId;
    private ResultActions response;

    @Dado("un estacionamiento registrado en la institución")
    public void aParkingLotRegisteredInTheInstitution() throws Exception {
        long institutionId = institution("Universidad");
        adminId = user(institutionId, "ADMIN", "admin@uni-" + suffix + ".test");
        lotId = registerParkingLot(adminId, "Estacionamiento Central " + suffix);
    }

    @Cuando("el administrador crea una nueva zona")
    public void theAdministratorCreatesAZone() throws Exception {
        response = postAs(adminId, "/api/v1/parking-lots/{lotId}/zones", """
                {"name": "Zona Norte"}
                """, lotId);
    }

    @Entonces("la zona queda asociada a ese estacionamiento.")
    public void theZoneBelongsToTheParkingLot() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.parkingLotId").value(lotId))
                .andExpect(jsonPath("$.name").value("Zona Norte"));
        getAs(adminId, "/api/v1/parking-lots/{lotId}/zones", lotId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) idOf(response))));
    }

    @Dado("que un espacio con la misma etiqueta ya existe dentro de la zona")
    public void aSpaceWithTheSameLabelExistsInTheZone() throws Exception {
        aParkingLotRegisteredInTheInstitution();
        zoneId = createZone();
        levelId = createLevel();
        createSpace()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.levelId").value(levelId))
                .andExpect(jsonPath("$.label").value(SPACE_LABEL));
    }

    @Cuando("el administrador intenta registrarlo nuevamente")
    public void theAdministratorTriesToRegisterItAgain() throws Exception {
        response = createSpace();
    }

    @Entonces("el sistema impide la duplicación.")
    public void theDuplicateIsRejected() throws Exception {
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Space label already exists in this level."));
        getAs(adminId, "/api/v1/levels/{levelId}/spaces", levelId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private long createZone() throws Exception {
        return idOf(postAs(adminId, "/api/v1/parking-lots/{lotId}/zones", """
                {"name": "Zona Norte"}
                """, lotId).andExpect(status().isCreated()));
    }

    private long createLevel() throws Exception {
        return idOf(postAs(adminId, "/api/v1/zones/{zoneId}/levels", """
                {"name": "Nivel 1"}
                """, zoneId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zoneId").value(zoneId)));
    }

    private ResultActions createSpace() throws Exception {
        return postAs(adminId, "/api/v1/levels/{levelId}/spaces", """
                {"label": "%s"}
                """.formatted(SPACE_LABEL), levelId);
    }
}
