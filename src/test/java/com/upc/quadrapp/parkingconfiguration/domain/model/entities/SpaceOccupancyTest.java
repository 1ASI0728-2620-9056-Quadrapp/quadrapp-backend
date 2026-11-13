package com.upc.quadrapp.parkingconfiguration.domain.model.entities;

import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceStatus;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.SpaceType;
import com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects.UnknownReason;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SpaceOccupancyTest {

    @Test
    void detectionUpdatesStateAndClearsUnknownReason() {
        Instant since = Instant.parse("2026-01-01T10:00:00Z");
        Instant occurredAt = Instant.parse("2026-01-01T10:05:00Z");
        Instant receivedAt = Instant.parse("2026-01-01T10:05:01Z");
        Space space = space(SpaceStatus.UNKNOWN, UnknownReason.NO_SENSOR, since, null);

        Space updated = space.applyOccupancyDetection(
                SpaceStatus.OCCUPIED,
                occurredAt,
                receivedAt
        );

        assertEquals(SpaceStatus.OCCUPIED, updated.getStatus());
        assertNull(updated.getUnknownReason());
        assertEquals(occurredAt, updated.getStatusSince());
        assertEquals(occurredAt, updated.getLastEventAt());
        assertEquals(receivedAt, updated.getUpdatedAt());
    }

    @Test
    void olderDetectionDoesNotChangeTheSpace() {
        Instant lastEventAt = Instant.parse("2026-01-01T10:05:00Z");
        Space space = space(
                SpaceStatus.OCCUPIED,
                null,
                lastEventAt,
                lastEventAt
        );

        Space updated = space.applyOccupancyDetection(
                SpaceStatus.FREE,
                lastEventAt.minusSeconds(1),
                lastEventAt.plusSeconds(1)
        );

        assertSame(space, updated);
    }

    @Test
    void sameStateDetectionAdvancesLastEventWithoutResettingStatusSince() {
        Instant statusSince = Instant.parse("2026-01-01T10:00:00Z");
        Instant lastEventAt = Instant.parse("2026-01-01T10:05:00Z");
        Instant occurredAt = lastEventAt.plusSeconds(10);
        Space space = space(
                SpaceStatus.FREE,
                null,
                statusSince,
                lastEventAt
        );

        Space updated = space.applyOccupancyDetection(
                SpaceStatus.FREE,
                occurredAt,
                occurredAt.plusSeconds(1)
        );

        assertEquals(statusSince, updated.getStatusSince());
        assertEquals(occurredAt, updated.getLastEventAt());
    }

    private static Space space(
            SpaceStatus status,
            UnknownReason reason,
            Instant statusSince,
            Instant lastEventAt
    ) {
        return Space.restore(
                1L,
                2L,
                "A1",
                SpaceType.STANDARD,
                null,
                null,
                status,
                reason,
                statusSince,
                lastEventAt,
                true,
                statusSince,
                statusSince
        );
    }
}
