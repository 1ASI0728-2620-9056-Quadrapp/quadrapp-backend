package com.upc.quadrapp.iam.domain.model.exceptions;

/**
 * A request that breaks a business rule of IAM given the current state (HTTP 409), such as an
 * email that is already registered. Any other {@link IllegalStateException} is a server fault.
 */
public class IamConflictException extends IllegalStateException {

    public IamConflictException(String message) {
        super(message);
    }
}
