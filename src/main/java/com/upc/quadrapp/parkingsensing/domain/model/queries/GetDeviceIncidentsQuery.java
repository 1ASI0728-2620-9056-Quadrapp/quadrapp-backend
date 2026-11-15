package com.upc.quadrapp.parkingsensing.domain.model.queries;

/**
 * Incidents of an institution, optionally of one parking lot and optionally only those with one
 * status; a {@code null} filter does not filter.
 */
public record GetDeviceIncidentsQuery(Long institutionId, Long parkingLotId, Status status) {

    /** An incident is open until it is resolved. */
    public enum Status { OPEN, RESOLVED }

    public GetDeviceIncidentsQuery {
        if (institutionId == null || institutionId <= 0) throw new IllegalArgumentException("institutionId must be positive.");
        if (parkingLotId != null && parkingLotId <= 0) throw new IllegalArgumentException("parkingLotId must be positive.");
    }
}
