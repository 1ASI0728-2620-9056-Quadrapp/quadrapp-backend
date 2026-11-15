package com.upc.quadrapp.parkingsensing.infrastructure.readings.memory;

import com.upc.quadrapp.parkingsensing.application.internal.outboundservices.readings.ReadingProcessingPort;
import com.upc.quadrapp.parkingsensing.domain.model.valueobjects.SensorReadingResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
@Profile("dev")
public class InMemoryReadingProcessingAdapter implements ReadingProcessingPort {
    private record DeviceKey(Long institutionId, String hardwareId) {}
    private record EventKey(Long institutionId, String hardwareId, String eventId) {}
    private final ConcurrentHashMap<DeviceKey, Object> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EventKey, SensorReadingResult> completed = new ConcurrentHashMap<>();

    @Override
    public SensorReadingResult processOnce(Long institutionId, String hardwareId,
                                           String eventId, Supplier<SensorReadingResult> operation) {
        var deviceKey = new DeviceKey(institutionId, hardwareId);
        var eventKey = new EventKey(institutionId, hardwareId, eventId);
        Object lock = locks.computeIfAbsent(deviceKey, ignored -> new Object());
        synchronized (lock) {
            SensorReadingResult existing = completed.get(eventKey);
            if (existing != null) return existing;
            SensorReadingResult result = operation.get();
            completed.put(eventKey, result);
            return result;
        }
    }
}