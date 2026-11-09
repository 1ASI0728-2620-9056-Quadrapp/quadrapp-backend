package com.upc.quadrapp.parkingconfiguration.domain.model.valueobjects;

import java.time.Instant;
import java.util.Objects;

public record EventWindow(
        Instant startAt,
        Instant endAt
) {
    public EventWindow {

        Objects.requireNonNull(
                startAt,
                "startAt is required."
        );

        Objects.requireNonNull(
                endAt,
                "endAt is required."
        );

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "Event end must be after event start."
            );
        }
    }

    public boolean overlaps(EventWindow other) {

        Objects.requireNonNull(
                other,
                "other window is required."
        );

        return startAt.isBefore(other.endAt())
                && other.startAt().isBefore(endAt);
    }
}
