package com.upc.quadrapp.parkingsensing.domain.model.aggregates;

import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.*;
import java.time.Instant;
import java.util.Objects;

public final class SensorFeed {
    private final Long deviceId;
    private final int minimumDetectionSeconds;
    private SensorState confirmedState;
    private SensorState pendingState;
    private Instant pendingSince;
    private Instant lastReadingAt;
    private boolean active;

    private SensorFeed(Long deviceId, int minimumDetectionSeconds,
                       SensorState confirmedState, SensorState pendingState,
                       Instant pendingSince, Instant lastReadingAt, boolean active) {
        if (deviceId == null || deviceId <= 0) throw new IllegalArgumentException("deviceId must be positive.");
        if (minimumDetectionSeconds < 1) throw new IllegalArgumentException("minimumDetectionSeconds must be positive.");
        if ((pendingState == null) != (pendingSince == null)) {
            throw new IllegalArgumentException("Pending state and timestamp must be present together.");
        }
        this.deviceId = deviceId;
        this.minimumDetectionSeconds = minimumDetectionSeconds;
        this.confirmedState = confirmedState;
        this.pendingState = pendingState;
        this.pendingSince = pendingSince;
        this.lastReadingAt = lastReadingAt;
        this.active = active;
    }

    public static SensorFeed open(Long deviceId, int minimumDetectionSeconds) {
        return new SensorFeed(deviceId, minimumDetectionSeconds, null, null, null, null, true);
    }

    public static SensorFeed restore(Long deviceId, int minimumDetectionSeconds,
                                     SensorState confirmedState, SensorState pendingState,
                                     Instant pendingSince, Instant lastReadingAt, boolean active) {
        return new SensorFeed(deviceId, minimumDetectionSeconds, confirmedState,
                pendingState, pendingSince, lastReadingAt, active);
    }

    public SensorReadingResult accept(SensorReading reading) {
        Objects.requireNonNull(reading, "reading is required.");
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
            pendingState = null;
            pendingSince = null;
            return new SensorReadingResult(discarded
                    ? ReadingOutcome.BRIEF_CHANGE_DISCARDED
                    : ReadingOutcome.UNCHANGED, null, null);
        }
        if (pendingState != state) {
            pendingState = state;
            pendingSince = reading.readingAt();
            return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
        }
        if (!reading.readingAt().isBefore(pendingSince.plusSeconds(minimumDetectionSeconds))) {
            confirmedState = state;
            pendingState = null;
            pendingSince = null;
            return new SensorReadingResult(ReadingOutcome.CONFIRMED, state, null);
        }
        return new SensorReadingResult(ReadingOutcome.PENDING, null, null);
    }

    /** Never confirm from silence: a further matching reading is required. */
    public void deactivate() { active = false; pendingState = null; pendingSince = null; }
    public void resetConfirmation() { confirmedState = null; pendingState = null; pendingSince = null; }
    public Long getDeviceId() { return deviceId; }
    public int getMinimumDetectionSeconds() { return minimumDetectionSeconds; }
    public SensorState getConfirmedState() { return confirmedState; }
    public SensorState getPendingState() { return pendingState; }
    public Instant getPendingSince() { return pendingSince; }
    public Instant getLastReadingAt() { return lastReadingAt; }
    public boolean isActive() { return active; }
}
