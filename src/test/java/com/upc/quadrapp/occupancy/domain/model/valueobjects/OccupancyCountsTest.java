package com.upc.quadrapp.occupancy.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OccupancyCountsTest {

    @Test
    void rejectsCountsThatDoNotAddUpToTotal() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OccupancyCounts(10, 4, 4, 1)
        );
    }
}
