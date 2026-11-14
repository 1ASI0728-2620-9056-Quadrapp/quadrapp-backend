package com.upc.quadrapp.occupancy.infrastructure.realtime;

import com.upc.quadrapp.occupancy.application.internal.outboundservices.realtime.OccupancyRealtimePort;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sends occupancy changes to the STOMP subscribers of the parking lot, at most one message per
 * zone per second (DD-04). A change of a quiet zone goes out at once; changes that arrive within
 * the same second, as when a gateway replays its buffer, are merged and only the last state of
 * the zone (with every space that changed) is sent when the second ends. Each change is sent after
 * its own commit, so they may arrive out of order: the zone counts always come from the newest
 * change (highest lot version), and a space never goes back to an older state.
 * <p>
 * The broker is in memory: each replica delivers the changes it processed to its own clients.
 */
@Component
public class ThrottledOccupancyBroadcaster implements OccupancyRealtimePort {

    static final Duration WINDOW = Duration.ofSeconds(1);

    private static final Logger LOGGER = LoggerFactory.getLogger(ThrottledOccupancyBroadcaster.class);

    /** Runs a task after a delay; replaced in tests to control time. */
    interface DelayedExecutor {
        void schedule(Runnable task, Duration delay);
    }

    private final SimpMessageSendingOperations messaging;
    private final Clock clock;
    private final DelayedExecutor executor;
    private final ScheduledExecutorService ownExecutor;
    private final Map<ZoneKey, ZoneWindow> windows = new ConcurrentHashMap<>();

    @Autowired
    public ThrottledOccupancyBroadcaster(
            SimpMessageSendingOperations messaging,
            @Qualifier("parkingSensingClock") Clock clock
    ) {
        this.messaging = messaging;
        this.clock = clock;
        this.ownExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "occupancy-realtime");
            thread.setDaemon(true);
            return thread;
        });
        this.executor = (task, delay) ->
                ownExecutor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    ThrottledOccupancyBroadcaster(SimpMessageSendingOperations messaging, Clock clock, DelayedExecutor executor) {
        this.messaging = messaging;
        this.clock = clock;
        this.executor = executor;
        this.ownExecutor = null;
    }

    @Override
    public void publish(ZoneOccupancyChange change) {
        ZoneKey key = new ZoneKey(change.parkingLotId(), change.zoneId());
        ZoneWindow window = windows.computeIfAbsent(key, ignored -> new ZoneWindow());
        OccupancyRealtimeMessage immediate = null;
        synchronized (window) {
            if (window.latest == null || change.lotVersion() > window.latest.lotVersion()) {
                window.latest = change;
            }
            Long spaceId = change.space().spaceId();
            Long spaceVersion = window.spaceVersions.get(spaceId);
            if (spaceVersion != null && spaceVersion >= change.lotVersion()) {
                return;
            }
            window.spaceVersions.put(spaceId, change.lotVersion());
            Instant now = clock.instant();
            boolean quiet = window.lastSentAt == null || !now.isBefore(window.lastSentAt.plus(WINDOW));
            if (window.pendingSpaces.isEmpty() && quiet) {
                window.lastSentAt = now;
                immediate = OccupancyRealtimeMessage.of(window.latest, List.of(change.space()));
            } else {
                window.pendingSpaces.put(spaceId, change.space());
                if (!window.flushScheduled) {
                    window.flushScheduled = true;
                    Duration delay = Duration.between(now, window.lastSentAt.plus(WINDOW));
                    executor.schedule(() -> flush(key), delay.isNegative() ? Duration.ZERO : delay);
                }
            }
        }
        if (immediate != null) {
            send(immediate);
        }
    }

    private void flush(ZoneKey key) {
        ZoneWindow window = windows.get(key);
        OccupancyRealtimeMessage message;
        synchronized (window) {
            window.flushScheduled = false;
            if (window.pendingSpaces.isEmpty()) {
                return;
            }
            message = OccupancyRealtimeMessage.of(window.latest, window.pendingSpaces.values());
            window.pendingSpaces.clear();
            window.lastSentAt = clock.instant();
        }
        send(message);
    }

    private void send(OccupancyRealtimeMessage message) {
        try {
            messaging.convertAndSend(OccupancyRealtimeMessage.destination(message.parkingLotId()), message);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not send the occupancy of zone {} to its subscribers.", message.zoneId(), exception);
        }
    }

    @PreDestroy
    void shutdown() {
        if (ownExecutor != null) {
            ownExecutor.shutdownNow();
        }
    }

    private record ZoneKey(Long parkingLotId, Long zoneId) {
        ZoneKey {
            Objects.requireNonNull(parkingLotId);
            Objects.requireNonNull(zoneId);
        }
    }

    /**
     * Throttling state of one zone. {@code latest} is the newest state received (highest lot
     * version), whatever the order of arrival; a space change older than the last one accepted
     * for that space is dropped.
     */
    private static final class ZoneWindow {
        private Instant lastSentAt;
        private ZoneOccupancyChange latest;
        private final Map<Long, Long> spaceVersions = new HashMap<>();
        private final Map<Long, SpaceChange> pendingSpaces = new LinkedHashMap<>();
        private boolean flushScheduled;
    }
}
