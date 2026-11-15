package com.upc.quadrapp.iam.application.internal.outboundservices.hashing;

public interface TokenHashingService {
    String hash(String purpose, String value);
}
