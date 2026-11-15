package com.upc.quadrapp.iam.application.internal.outboundservices.mail;

public interface OtpDeliveryService {
    void send(String email, String code);
}
