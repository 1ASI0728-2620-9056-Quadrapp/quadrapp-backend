package com.upc.quadrapp.parkingsensing.infrastructure.readings.memory;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.SensorFeedStatePort;
import com.upc.quadrapp.parkingsensing.domain.model.aggregates.SensorFeed;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReading;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Profile("dev")
public class InMemorySensorFeedStateAdapter implements SensorFeedStatePort {
    private final ConcurrentHashMap<Long, SensorFeed> feeds = new ConcurrentHashMap<>();

    @Override
    public SensorReadingResult accept(Long deviceId, int minimumDetectionSeconds, SensorReading reading) {
        AtomicReference<SensorReadingResult> result = new AtomicReference<>();
        feeds.compute(deviceId, (id, current) -> {
            SensorFeed feed = current == null
                    ? SensorFeed.open(id, minimumDetectionSeconds)
                    : current;
            if (feed.getMinimumDetectionSeconds() != minimumDetectionSeconds) {
                // Do not apply a new threshold to an old pending transition.
                feed = SensorFeed.open(id, minimumDetectionSeconds);
            }
            result.set(feed.accept(reading));
            return feed;
        });
        return result.get();
    }

    @Override
    public void reset(Long deviceId) {
        feeds.remove(deviceId);
    }

    @Override
    public void deactivate(Long deviceId) {
        feeds.computeIfPresent(deviceId, (id, feed) -> {
            feed.deactivate();
            return feed;
        });
    }
}