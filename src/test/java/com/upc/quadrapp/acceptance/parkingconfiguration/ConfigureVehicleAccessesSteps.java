package com.upc.quadrapp.acceptance.parkingconfiguration;

import com.upc.quadrapp.acceptance.AcceptanceSteps;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US19: the administrator configures the entry and exit accesses of a parking lot. */
public class ConfigureVehicleAccessesSteps extends AcceptanceSteps {

    private long adminId;
    private long lotId;
    private ResultActions response;

    @Dado("que el administrador configura un nuevo acceso")
    public void theAdministratorConfiguresANewAccess() throws Exception {
        long institutionId = institution("Universidad");
        adminId = user(institutionId, "ADMIN", "admin@uni-" + suffix + ".test");
        lotId = registerParkingLot(adminId, "Estacionamiento Central " + suffix);
    }

    @Cuando("proporciona sus datos")
    public void providesItsData() throws Exception {
        response = postAs(adminId, "/api/v1/parking-lots/{lotId}/accesses", """
                {"name": "Puerta Principal", "type": "ENTRY"}
                """, lotId);
    }

    @Entonces("el acceso queda asociado al estacionamiento correspondiente.")
    public void theAccessBelongsToTheParkingLot() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.parkingLotId").value(lotId))
                .andExpect(jsonPath("$.name").value("Puerta Principal"))
                .andExpect(jsonPath("$.type").value("ENTRY"));
        getAs(adminId, "/api/v1/parking-lots/{lotId}/accesses", lotId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) idOf(response))));
    }
}
