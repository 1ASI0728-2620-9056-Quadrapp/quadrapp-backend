package com.upc.quadrapp.iam.interfaces.rest;

import com.upc.quadrapp.iam.domain.model.exceptions.IamConflictException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IamExceptionHandlerTest {

    private final IamExceptionHandler handler = new IamExceptionHandler();

    @Test
    void businessConflictIs409WithItsMessage() {
        var response = handler.handleConflict(new IamConflictException("Email is already registered."));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).containsEntry("message", "Email is already registered.");
    }

    @Test
    void internalStateErrorIs500WithoutItsInternalMessage() {
        var response = handler.handleInvalidState(new IllegalStateException("Unable to hash token."));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("message", "An unexpected error occurred.");
    }
}
