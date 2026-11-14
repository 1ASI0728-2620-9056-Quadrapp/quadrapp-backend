package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MQTT 5 connection that feeds the gateway readings to {@link MqttInboundListener}.
 * <p>
 * It subscribes with QoS 1 to the shared subscription {@code $share/<group>/quadrapp/+/+/readings},
 * so the broker delivers each reading to one replica of the group. Acknowledgements are manual:
 * a message is confirmed only after the listener processed or stored it, otherwise the broker
 * delivers it again after a reconnection. Messages arrive one at a time on the client thread.
 * <p>
 * The client reconnects on its own after losing the connection and subscribes again on every
 * connection. If the broker is unreachable at startup, the first connection is retried in the
 * background, so the application starts anyway.
 */
public class MqttReadingSubscriber implements SmartLifecycle, MqttCallback {

    static final int QOS = 1;
    private static final long SESSION_EXPIRY_SECONDS = 3_600;
    private static final long FIRST_CONNECTION_RETRY_SECONDS = 5;
    private static final long DISCONNECT_TIMEOUT_MILLIS = 5_000;

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttReadingSubscriber.class);

    private final MqttIngestionProperties properties;
    private final MqttInboundListener listener;
    private final ScheduledExecutorService firstConnection = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("mqtt-first-connection").daemon().factory());
    private MqttAsyncClient client;
    private volatile boolean running;
    private volatile boolean subscribed;

    public MqttReadingSubscriber(MqttIngestionProperties properties, MqttInboundListener listener) {
        this.properties = properties;
        this.listener = listener;
    }

    @Override
    public synchronized void start() {
        String clientId = properties.hasFixedClientId()
                ? properties.clientId()
                : "quadrapp-backend-" + UUID.randomUUID();
        try {
            client = new MqttAsyncClient(properties.url(), clientId, new MemoryPersistence());
        } catch (MqttException invalid) {
            throw new IllegalStateException("Invalid MQTT configuration for " + properties.url(), invalid);
        }
        client.setManualAcks(true);
        client.setCallback(this);
        running = true;
        connect();
    }

    private void connect() {
        if (!running) {
            return;
        }
        try {
            client.connect(connectionOptions(), null, new MqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    // connectComplete subscribes.
                }

                @Override
                public void onFailure(IMqttToken token, Throwable failure) {
                    retryFirstConnection(failure);
                }
            });
        } catch (MqttException failure) {
            retryFirstConnection(failure);
        }
    }

    private void retryFirstConnection(Throwable failure) {
        if (!running) {
            return;
        }
        LOGGER.warn("Could not connect to the MQTT broker {}: {}. Retrying in {} s",
                properties.url(), failure.toString(), FIRST_CONNECTION_RETRY_SECONDS);
        firstConnection.schedule(this::connect, FIRST_CONNECTION_RETRY_SECONDS, TimeUnit.SECONDS);
    }

    private MqttConnectionOptions connectionOptions() {
        var options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setAutomaticReconnectDelay(1, 30);
        // A fixed client id keeps the session, and the readings queued for it, across restarts.
        // A random id would leave an orphan session behind on every restart, so it starts clean.
        options.setCleanStart(!properties.hasFixedClientId());
        options.setSessionExpiryInterval(properties.hasFixedClientId() ? SESSION_EXPIRY_SECONDS : 0L);
        if (properties.username() != null && !properties.username().isBlank()) {
            options.setUserName(properties.username());
            if (properties.password() != null) {
                options.setPassword(properties.password().getBytes(StandardCharsets.UTF_8));
            }
        }
        return options;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        String subscription = properties.readingsSubscription();
        LOGGER.info("Connected to the MQTT broker {}; subscribing to {}", serverUri, subscription);
        try {
            // Asynchronous on purpose: waiting for the token on the callback thread would block it.
            client.subscribe(new MqttSubscription[]{new MqttSubscription(subscription, QOS)}, null, new MqttActionListener() {
                @Override
                public void onSuccess(IMqttToken token) {
                    subscribed = true;
                    LOGGER.info("Subscribed to {}", subscription);
                }

                @Override
                public void onFailure(IMqttToken token, Throwable failure) {
                    LOGGER.error("Could not subscribe to {}", subscription, failure);
                }
            }, (MqttProperties) null);
        } catch (MqttException failure) {
            LOGGER.error("Could not subscribe to {}", subscription, failure);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            listener.onReading(topic, message.getPayload(), () -> acknowledge(message));
        } catch (RuntimeException failure) {
            // Not acknowledged: the broker delivers it again after the next reconnection.
            LOGGER.error("Reading on {} was neither processed nor stored; it stays unacknowledged", topic, failure);
        }
    }

    private void acknowledge(MqttMessage message) {
        try {
            client.messageArrivedComplete(message.getId(), message.getQos());
        } catch (MqttException failure) {
            throw new IllegalStateException("Could not acknowledge MQTT message " + message.getId(), failure);
        }
    }

    @Override
    public void disconnected(MqttDisconnectResponse response) {
        subscribed = false;
        if (running) {
            LOGGER.warn("Disconnected from the MQTT broker: {}", response);
        }
    }

    @Override
    public void mqttErrorOccurred(MqttException failure) {
        LOGGER.error("MQTT error", failure);
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        // The backend only subscribes.
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {
        // Enhanced authentication is not used.
    }

    @Override
    public synchronized void stop() {
        running = false;
        subscribed = false;
        firstConnection.shutdownNow();
        if (client == null) {
            return;
        }
        try {
            if (client.isConnected()) {
                client.disconnect(DISCONNECT_TIMEOUT_MILLIS).waitForCompletion(DISCONNECT_TIMEOUT_MILLIS);
            }
            client.close(true);
        } catch (MqttException failure) {
            LOGGER.warn("Could not close the MQTT connection cleanly: {}", failure.toString());
        }
    }

    /** Whether the backend is connected and subscribed, so it is receiving readings. */
    public boolean isSubscribed() {
        return subscribed && client != null && client.isConnected();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
