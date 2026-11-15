package com.upc.quadrapp.iam.infrastructure.mail.smtp.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.mail.InvitationDeliveryService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SmtpInvitationDeliveryService implements InvitationDeliveryService {

    private final JavaMailSender sender;

    @Value("${quadrapp.mail.from}")
    private String from;

    @Value("${quadrapp.iam.invitation-url-base}")
    private String invitationUrlBase;

    @Override
    public void send(
            String email,
            String invitationToken
    ) {

        String separator = invitationUrlBase.contains("?")
                ? "&"
                : "?";

        String link = invitationUrlBase +
                separator +
                "token=" +
                URLEncoder.encode(
                        invitationToken,
                        StandardCharsets.UTF_8
                );


        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(email);
        message.setSubject(
                "Quadrapp - Invitación institucional"
        );

        message.setText(
                "Has recibido una invitación para Quadrapp. " +
                        "Acepta aquí: " + link
        );

        sender.send(message);
    }
}
