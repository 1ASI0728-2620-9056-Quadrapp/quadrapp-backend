package com.upc.quadrapp.iam.application.internal.outboundservices.mail;

public interface InvitationDeliveryService {
    void send(String email, String invitationToken);
}
