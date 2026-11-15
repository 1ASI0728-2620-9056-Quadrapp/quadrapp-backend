package com.upc.quadrapp.prediction.infrastructure.realtime;

import com.upc.quadrapp.prediction.application.internal.outboundservices.realtime.ForecastRealtimePort;
import com.upc.quadrapp.prediction.domain.model.aggregates.Forecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sends the forecasts of each run to {@code /topic/parking-lots/{lotId}/forecasts} (the STOMP
 * endpoint and its authorization are the ones of Occupancy's real-time channel). The message
 * leaves only after the run commits, so a subscriber never sees a run that was rolled back.
 */
@Component
public class StompForecastBroadcaster implements ForecastRealtimePort {

    private static final Logger LOGGER = LoggerFactory.getLogger(StompForecastBroadcaster.class);

    private final SimpMessagingTemplate messaging;

    public StompForecastBroadcaster(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void forecastsGenerated(Long parkingLotId, List<Forecast> forecasts) {
        if (forecasts.isEmpty()) {
            return;
        }
        ForecastRunMessage message = ForecastRunMessage.of(parkingLotId, forecasts);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(message);
                }
            });
        } else {
            send(message);
        }
    }

    private void send(ForecastRunMessage message) {
        try {
            messaging.convertAndSend("/topic/parking-lots/" + message.parkingLotId() + "/forecasts", message);
        } catch (RuntimeException exception) {
            LOGGER.warn("Forecast notice for parking lot {} was not sent.", message.parkingLotId(), exception);
        }
    }

    public record ForecastRunMessage(
            Long parkingLotId,
            UUID runId,
            String trigger,
            Instant generatedAt,
            List<ForecastItem> forecasts
    ) {
        static ForecastRunMessage of(Long parkingLotId, List<Forecast> forecasts) {
            Forecast first = forecasts.getFirst();
            return new ForecastRunMessage(
                    parkingLotId,
                    first.runId(),
                    first.trigger() == null ? null : first.trigger().name(),
                    first.generatedAt(),
                    forecasts.stream().map(ForecastItem::of).toList()
            );
        }
    }

    public record ForecastItem(
            Long id,
            int horizonMinutes,
            Instant targetAt,
            BigDecimal expectedOccupancyPct,
            BigDecimal confidence,
            String confidenceLevel,
            String method,
            String modelVersion,
            Instant saturationExpectedAt
    ) {
        static ForecastItem of(Forecast forecast) {
            return new ForecastItem(
                    forecast.id(),
                    forecast.horizonMinutes(),
                    forecast.targetAt(),
                    forecast.expectedOccupancyPct(),
                    forecast.confidence(),
                    forecast.confidenceLevel().name(),
                    forecast.method().name(),
                    forecast.modelVersion(),
                    forecast.saturationExpectedAt()
            );
        }
    }
}
