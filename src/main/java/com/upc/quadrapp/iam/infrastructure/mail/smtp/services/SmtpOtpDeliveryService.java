package com.upc.quadrapp.iam.infrastructure.mail.smtp.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.mail.OtpDeliveryService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpOtpDeliveryService implements OtpDeliveryService {

    private final JavaMailSender sender;

    @Value("${quadrapp.mail.from}")
    private String from;

    @Override
    public void send(String email, String code) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(email);
        message.setSubject(
                "Quadrapp - Código de acceso"
        );

        message.setText(
                "Tu código de acceso es: " + code +
                        "\nCaduca pronto. Si no lo solicitaste, ignora este correo."
        );

        sender.send(message);
    }
}
