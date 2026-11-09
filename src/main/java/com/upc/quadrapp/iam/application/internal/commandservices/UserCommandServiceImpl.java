package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.hashing.TokenHashingService;
import com.upc.quadrapp.iam.application.internal.outboundservices.mail.InvitationDeliveryService;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionDomainPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.UserPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.commands.AcceptInvitationCommand;
import com.upc.quadrapp.iam.domain.model.commands.InviteUserCommand;
import com.upc.quadrapp.iam.domain.model.commands.UpdateNotificationPreferencesCommand;
import com.upc.quadrapp.iam.domain.model.valueobjects.EmailAddress;
import com.upc.quadrapp.iam.domain.model.valueobjects.UserStatus;
import com.upc.quadrapp.iam.domain.services.UserCommandService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class UserCommandServiceImpl implements UserCommandService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserCommandServiceImpl.class);

    private final UserPersistencePort userPersistencePort;
    private final InstitutionPersistencePort institutionPersistencePort;
    private final InstitutionDomainPersistencePort institutionDomainPersistencePort;

    private final IamAuthorizationService authorizationService;
    private final SecureTokenGenerator secureTokenGenerator;
    private final TokenHashingService tokenHashingService;
    private final InvitationDeliveryService invitationDeliveryService;

    @Value("${quadrapp.iam.invitation-expiration-hours:48}")
    private long invitationExpirationHours;

    @Override
    @Transactional
    public User handle(InviteUserCommand command) {
        var actor = authorizationService.requireActor(
                command.invitedByUserId()
        );

        authorizationService.requireInstitutionAdmin(
                actor,
                command.institutionId()
        );

        var institution = institutionPersistencePort
                .findById(command.institutionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Institution not found."
                ));

        if (!institution.isActive()) {
            throw new IllegalStateException(
                    "Institution is not active."
            );
        }

        EmailAddress email = new EmailAddress(command.email());

        if (userPersistencePort.existsByEmail(email.value())) {
            throw new IllegalStateException(
                    "Email is already registered."
            );
        }

        var institutionalDomain = institutionDomainPersistencePort
                .findByDomain(email.domain())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Email domain is not registered."
                ));

        if (!institutionalDomain.isVerified() ||
                !institutionalDomain.getInstitutionId()
                        .equals(command.institutionId())) {
            throw new IllegalArgumentException(
                    "Email domain is not verified for this institution."
            );
        }

        if (invitationExpirationHours <= 0) {
            throw new IllegalStateException(
                    "Invalid invitation expiration configuration."
            );
        }

        Instant now = Instant.now();

        String invitationToken = secureTokenGenerator.generate();

        String invitationHash = tokenHashingService.hash(
                "INVITATION",
                invitationToken
        );

        User user = User.invite(
                command.institutionId(),
                actor.getId(),
                email.value(),
                command.role(),
                invitationHash,
                now.plus(invitationExpirationHours, ChronoUnit.HOURS),
                now
        );

        User savedUser = userPersistencePort.save(user);

        sendInvitationAfterCommit(
                savedUser.getEmail().value(),
                invitationToken
        );

        return savedUser;
    }

    @Override
    @Transactional
    public User handle(AcceptInvitationCommand command) {
        if (command.invitationToken() == null ||
                command.invitationToken().isBlank()) {
            throw new IllegalArgumentException(
                    "Invitation token is required."
            );
        }

        String invitationHash = tokenHashingService.hash(
                "INVITATION",
                command.invitationToken()
        );

        User user = userPersistencePort
                .findByInvitationTokenHash(invitationHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid invitation token."
                ));

        var institution = institutionPersistencePort
                .findById(user.getInstitutionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Institution was not found."
                ));

        if (!institution.isActive()) {
            throw new IllegalStateException(
                    "Institution is not active."
            );
        }

        var institutionalDomain = institutionDomainPersistencePort
                .findByDomain(user.getEmail().domain())
                .orElseThrow(() -> new IllegalStateException(
                        "Institutional domain was not found."
                ));

        if (!institutionalDomain.isVerified() ||
                !institutionalDomain.getInstitutionId()
                        .equals(user.getInstitutionId())) {
            throw new IllegalStateException(
                    "Institutional domain is not verified."
            );
        }

        user.acceptInvitation(
                invitationHash,
                command.fullName(),
                Instant.now()
        );

        return userPersistencePort.save(user);
    }

    @Override
    @Transactional
    public User handle(UpdateNotificationPreferencesCommand command) {
        var actor = authorizationService.requireAuthenticatedUser();

        authorizationService.requireSelf(
                actor,
                command.userId()
        );

        User user = userPersistencePort
                .findById(command.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found."
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException(
                    "User is not active."
            );
        }

        user.updateNotificationPreferences(
                command.preferences(),
                Instant.now()
        );

        return userPersistencePort.save(user);
    }

    private void sendInvitationAfterCommit(
            String email,
            String invitationToken
    ) {
        if (!TransactionSynchronizationManager
                .isActualTransactionActive()) {
            throw new IllegalStateException(
                    "An active transaction is required."
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        try {
                            invitationDeliveryService.send(
                                    email,
                                    invitationToken
                            );
                        } catch (RuntimeException exception) {
                            LOGGER.error(
                                    "Invitation email delivery failed after commit.",
                                    exception
                            );
                        }
                    }
                }
        );
    }
}

