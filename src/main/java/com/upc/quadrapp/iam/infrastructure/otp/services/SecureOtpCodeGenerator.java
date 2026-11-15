package com.upc.quadrapp.iam.infrastructure.otp.services;

import com.upc.quadrapp.iam.application.internal.outboundservices.otp.OtpCodeGenerator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class SecureOtpCodeGenerator implements OtpCodeGenerator {
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        return String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
    }
}
