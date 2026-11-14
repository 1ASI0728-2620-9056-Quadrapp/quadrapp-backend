package com.upc.quadrapp.parkingsensing.infrastructure.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection to the MQTT broker that receives the gateway readings ({@code quadrapp.mqtt.*}).
 *
 * @param enabled     connects to the broker on startup; {@code false} lets the application run without one
 * @param url         broker URL, for example {@code tcp://localhost:1883} or {@code ssl://broker:8883}
 * @param username    backend credential (subscribe only); blank connects anonymously
 * @param password    password of that credential
 * @param clientId    MQTT client id, unique per replica; when set, the session survives restarts.
 *                    Blank uses a random id and a clean session
 * @param sharedGroup MQTT 5 shared subscription group: replicas in one group receive each message once
 */
@ConfigurationProperties("quadrapp.mqtt")
public record MqttIngestionProperties(
        boolean enabled,
        String url,
        String username,
        String password,
        String clientId,
        String sharedGroup
) {
    public MqttIngestionProperties {
        if (url == null || url.isBlank()) {
            url = "tcp://localhost:1883";
        }
        if (sharedGroup == null || sharedGroup.isBlank()) {
            sharedGroup = "quadrapp-backend";
        }
    }

    /** Subscription that shares {@code quadrapp/+/+/readings} among the replicas of the group. */
    public String readingsSubscription() {
        return "$share/" + sharedGroup + "/quadrapp/+/+/readings";
    }

    public boolean hasFixedClientId() {
        return clientId != null && !clientId.isBlank();
    }
}
