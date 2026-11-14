package com.upc.quadrapp.iam.domain.model.exceptions;

/**
 * The same email asked for more access codes than the window allows (CON-09, scenario 2).
 */
public class OtpRequestLimitExceededException extends RuntimeException {

    public OtpRequestLimitExceededException() {
        super("Too many access code requests. Try again later.");
    }
}
