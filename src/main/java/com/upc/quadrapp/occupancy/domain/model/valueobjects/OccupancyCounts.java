package com.upc.quadrapp.occupancy.domain.model.valueobjects;

public record OccupancyCounts(
        long totalSpaces,
        long freeCount,
        long occupiedCount,
        long unknownCount
) {
    public OccupancyCounts {
        if (totalSpaces < 0 || freeCount < 0 || occupiedCount < 0 || unknownCount < 0) {
            throw new IllegalArgumentException("Occupancy counts cannot be negative.");
        }

        if (freeCount + occupiedCount + unknownCount != totalSpaces) {
            throw new IllegalArgumentException(
                    "Free, occupied, and unknown counts must equal total spaces."
            );
        }
    }

    public void requireSmallintRange() {
        if (totalSpaces > Short.MAX_VALUE
                || freeCount > Short.MAX_VALUE
                || occupiedCount > Short.MAX_VALUE
                || unknownCount > Short.MAX_VALUE) {
            throw new IllegalStateException(
                    "Zone space counts exceed the database SMALLINT range."
            );
        }
    }
}
