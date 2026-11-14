package com.upc.quadrapp.parkingconfiguration.domain.model.aggregates;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Operating settings follow the ranges of OperatingSettings in the report: saturation 50-100 %,
 * flow window 5-60 min, minimum detection time 3-60 s and accuracy margin 1-30 points.
 */
class ParkingLotSettingsTest {

    @ParameterizedTest(name = "saturation={0} flow={1} accuracy={2} occupied={3} freed={4}")
    @CsvSource({
            "49, 15, 10, 10, 20",
            "101, 15, 10, 10, 20",
            "90, 4, 10, 10, 20",
            "90, 61, 10, 10, 20",
            "90, 15, 0, 10, 20",
            "90, 15, 31, 10, 20",
            "90, 15, 10, 2, 20",
            "90, 15, 10, 61, 20",
            "90, 15, 10, 10, 2",
            "90, 15, 10, 10, 40000"
    })
    void settingsOutsideTheReportRangesAreRejected(int saturation, int flow, int accuracy, int occupied, int freed) {
        assertThatThrownBy(() -> register(saturation, flow, accuracy, occupied, freed))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid parking lot settings.");
    }

    @ParameterizedTest(name = "saturation={0} flow={1} accuracy={2} occupied={3} freed={4}")
    @CsvSource({
            "50, 5, 1, 3, 3",
            "100, 60, 30, 60, 60",
            "90, 15, 10, 10, 20"
    })
    void settingsAtTheLimitsOfTheRangesAreAccepted(int saturation, int flow, int accuracy, int occupied, int freed) {
        assertThatCode(() -> register(saturation, flow, accuracy, occupied, freed)).doesNotThrowAnyException();
    }

    private static ParkingLot register(int saturation, int flow, int accuracy, int occupied, int freed) {
        return ParkingLot.register(1L, 1L, "Main lot", null, null, null, null, null,
                saturation, 15, 180, flow, accuracy, occupied, freed);
    }
}
