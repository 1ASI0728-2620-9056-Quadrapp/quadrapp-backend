package com.upc.quadrapp.acceptance.parkingconfiguration;

import com.upc.quadrapp.acceptance.AcceptanceSteps;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US16: an institution administrator registers the parking lots of the university. */
public class RegisterParkingLotSteps extends AcceptanceSteps {

    private long institutionId;
    private long adminId;
    private String lotName;
    private String requestBody;
    private ResultActions response;

    @Dado("que el administrador proporciona los datos obligatorios")
    public void theAdministratorProvidesTheRequiredData() {
        anInstitutionAdministrator();
        lotName = "Estacionamiento Central " + suffix;
        requestBody = """
                {"name": "%s", "address": "Av. Universitaria 1801", "latitude": -12.0464, "longitude": -77.0428}
                """.formatted(lotName);
    }

    @Cuando("registra un estacionamiento")
    public void registersAParkingLot() throws Exception {
        response = postAs(adminId, "/api/v1/parking-lots", requestBody);
    }

    @Entonces("el sistema crea el estacionamiento correctamente.")
    public void theParkingLotIsCreated() throws Exception {
        response.andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(lotName))
                .andExpect(jsonPath("$.institutionId").value(institutionId))
                .andExpect(jsonPath("$.createdBy").value(adminId))
                .andExpect(jsonPath("$.active").value(true));
        getAs(adminId, "/api/v1/parking-lots/{id}", idOf(response))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(lotName));
    }

    @Dado("que la institución ya tiene un estacionamiento registrado con el mismo nombre")
    public void theInstitutionAlreadyHasAParkingLotWithTheSameName() throws Exception {
        anInstitutionAdministrator();
        lotName = "Estacionamiento Norte " + suffix;
        registerParkingLot(adminId, lotName);
    }

    @Cuando("el administrador intenta registrar otro con ese nombre")
    public void theAdministratorTriesToRegisterAnotherWithThatName() throws Exception {
        response = postAs(adminId, "/api/v1/parking-lots", """
                {"name": "%s"}
                """.formatted(lotName));
    }

    @Entonces("el sistema impide la duplicación dentro de esa institución.")
    public void theDuplicateIsRejectedWithinTheInstitution() throws Exception {
        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Parking lot name already exists in this institution."));
        getAs(adminId, "/api/v1/parking-lots")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        long otherInstitution = institution("Otra Universidad");
        long otherAdmin = user(otherInstitution, "ADMIN", "admin@other-" + suffix + ".test");
        postAs(otherAdmin, "/api/v1/parking-lots", """
                {"name": "%s"}
                """.formatted(lotName))
                .andExpect(status().isCreated());
    }

    private void anInstitutionAdministrator() {
        institutionId = institution("Universidad");
        adminId = user(institutionId, "ADMIN", "admin@uni-" + suffix + ".test");
    }
}
