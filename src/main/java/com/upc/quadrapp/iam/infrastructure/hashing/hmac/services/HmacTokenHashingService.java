package com.upc.quadrapp.iam.infrastructure.hashing.hmac.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.hashing.TokenHashingService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

@Component
public class HmacTokenHashingService implements TokenHashingService {
    private final SecretKeySpec key;

    public HmacTokenHashingService(@Value("${quadrapp.security.hash-pepper-base64}") String base64Key
    ) {
        byte[] bytes = Base64.getDecoder().decode(
                base64Key
        );

        if (bytes.length < 32) {
            throw new IllegalArgumentException("Hash pepper needs at least 32 bytes.");
        }

        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Override
    public String hash(String purpose, String value) {

        if (purpose == null ||
                purpose.isBlank() ||
                value == null ||
                value.isBlank()) {
            throw new IllegalArgumentException(
                    "Token hash purpose and value are required."
            );
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);

            String data = purpose + ":" + value;

            byte[] hash = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to hash token.",
                    ex
            );
        }
    }
}
