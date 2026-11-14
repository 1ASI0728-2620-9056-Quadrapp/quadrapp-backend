package com.upc.quadrapp.parkingsensing.domain.model.aggregates;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import java.time.Instant;
import java.util.Objects;

public final class SensorFeed {
    private final Long deviceId;
    private SensorState confirmedState;
    private SensorState pendingState;
    private Instant pendingSince;
    /** Minimum detection time of the pending transition, fixed when it started. */
    private Integer pendingMinimumSeconds;
    private Instant lastReadingAt;
    private boolean active;

    private SensorFeed(Long deviceId, SensorState confirmedState, SensorState pendingState,
                       Instant pendingSince, Integer pendingMinimumSeconds,
                       Instant lastReadingAt, boolean active) {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId must be positive.");
        if ((pendingState == null) != (pendingSince == null)
                || (pendingState == null) != (pendingMinimumSeconds == null)) {
            throw new IllegalArgumentException("Pending state, timestamp and minimum time must be present together.");
        }
        if (pendingMinimumSeconds != null && pendingMinimumSeconds < 1) {
            throw new IllegalArgumentException("Minimum detection time must be positive.");
        }
        this.deviceId = deviceId;
        this.confirmedState = confirmedState;
        this.pendingState = pendingState;
        this.pendingSince = pendingSince;
        this.pendingMinimumSeconds = pendingMinimumSeconds;
        this.lastReadingAt = lastReadingAt;
        this.active = active;
    }

    public static SensorFeed open(Long deviceId) {
        return new SensorFeed(deviceId, null, null, null, null, null, true);
    }

    public static SensorFeed restore(Long deviceId, SensorState confirmedState, SensorState pendingState,
                                     Instant pendingSince, Integer pendingMinimumSeconds,
                                     Instant lastReadingAt, boolean active) {
        return new SensorFeed(deviceId, confirmedState, pendingState, pendingSince,
                pendingMinimumSeconds, lastReadingAt, active);
    }

    /**
     * Applies a reading. {@code minimumDetectionSeconds} is the minimum time of the state the
     * reading reports (occupied and freed can differ). A pending transition keeps the time it
     * started with, so a configuration change never applies to it halfway.
     */
    public SensorReadingResult accept(SensorReading reading, int minimumDetectionSeconds) {
        Objects.requireNonNull(reading, "reading is required.");
        if (minimumDetectionSeconds < 1) throw new IllegalArgumentException("Minimum detection time must be positive.");
        if (!active) throw new IllegalStateException("Inactive sensor feed.");
        if (lastReadingAt != null && !reading.readingAt().isAfter(lastReadingAt)) {
            return new SensorReadingResult(ReadingOutcome.OUT_OF_ORDER, null, null);
        }
        lastReadingAt = reading.readingAt();
        if (reading.kind() == ReadingKind.HEARTBEAT) {
            return new SensorReadingResult(ReadingOutcome.HEARTBEAT_ACCEPTED, null, null);
        }
        if (reading.kind() == ReadingKind.PASS) {
            return new SensorReadingResult(ReadingOutcome.PASSAGE_RECORDED,
                    null, reading.direction());
        }

        SensorState state = reading.state();
        if (state == confirmedState) {
            boolean discarded = pendingState != null;
            clearPending();
            return new SensorReadingResult(discarded
                    ? ReadingOutcome.BRIEF_CHANGE_DISCARDED
                    : ReadingOutcome.UNCHANGED, null, null);
        }
        if (pendingState != state) {
            pendingState = state;
            pendingSince = reading.readingAt();
            pendingMinimumSeconds = minimumDetectionSeconds;
            return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
        }
        if (!reading.readingAt().isBefore(pendingSince.plusSeconds(pendingMinimumSeconds))) {
            confirmedState = state;
            clearPending();
            return new SensorReadingResult(ReadingOutcome.CONFIRMED, state, null);
        }
        return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
    }

    /** Never confirm from silence: a further matching reading is required. */
    public void deactivate() { active = false; clearPending(); }
    public void resetConfirmation() { confirmedState = null; clearPending(); }
    private void clearPending() { pendingState = null; pendingSince = null; pendingMinimumSeconds = null; }
    public Long getDeviceId() { return deviceId; }
    public Integer getPendingMinimumSeconds() { return pendingMinimumSeconds; }
    public SensorState getConfirmedState() { return confirmedState; }
    public SensorState getPendingState() { return pendingState; }
    public Instant getPendingSince() { return pendingSince; }
    public Instant getLastReadingAt() { return lastReadingAt; }
    public boolean isActive() { return active; }
}
