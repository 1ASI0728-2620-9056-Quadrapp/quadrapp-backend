package com.upc.quadrapp.iam.application.internal.commandservices;

import com.upc.quadrapp.iam.application.internal.outboundservices.hashing.TokenHashingService;
import com.upc.quadrapp.iam.application.internal.outboundservices.mail.OtpDeliveryService;
import com.upc.quadrapp.iam.application.internal.outboundservices.otp.OtpChallengeStore;
import com.upc.quadrapp.iam.application.internal.outboundservices.otp.OtpCodeGenerator;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionDomainPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.InstitutionPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.RefreshTokenPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.persistence.UserPersistencePort;
import com.upc.quadrapp.iam.application.internal.outboundservices.tokens.AccessTokenService;
import com.upc.quadrapp.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;

import com.upc.quadrapp.iam.domain.model.aggregates.User;
import com.upc.quadrapp.iam.domain.model.commands.RefreshSessionCommand;
import com.upc.quadrapp.iam.domain.model.commands.RequestOtpCommand;
import com.upc.quadrapp.iam.domain.model.commands.SignInCommand;
import com.upc.quadrapp.iam.domain.model.commands.SignOutCommand;
import com.upc.quadrapp.iam.domain.model.entities.RefreshToken;
import com.upc.quadrapp.iam.domain.model.exceptions.OtpRequestLimitExceededException;
import com.upc.quadrapp.iam.domain.model.valueobjects.EmailAddress;
import com.upc.quadrapp.iam.domain.model.valueobjects.Roles;
import com.upc.quadrapp.iam.domain.model.valueobjects.UserStatus;
import com.upc.quadrapp.iam.domain.services.AuthenticationCommandService;
import com.upc.quadrapp.iam.domain.services.AuthenticationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationCommandServiceImpl
        implements AuthenticationCommandService {

    private final UserPersistencePort userPersistencePort;
    private final InstitutionPersistencePort institutionPersistencePort;
    private final InstitutionDomainPersistencePort institutionDomainPersistencePort;
    private final RefreshTokenPersistencePort refreshTokenPersistencePort;

    private final OtpCodeGenerator otpCodeGenerator;
    private final OtpChallengeStore otpChallengeStore;
    private final OtpDeliveryService otpDeliveryService;

    private final TokenHashingService tokenHashingService;
    private final SecureTokenGenerator secureTokenGenerator;
    private final AccessTokenService accessTokenService;

    @Value("${quadrapp.iam.otp-expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${quadrapp.iam.otp-max-requests:5}")
    private int otpMaxRequests;

    @Value("${quadrapp.iam.otp-request-window-minutes:15}")
    private long otpRequestWindowMinutes;

    @Value("${quadrapp.iam.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    @Override
    public void handle(RequestOtpCommand command) {
        EmailAddress email = new EmailAddress(command.email());
        Instant now = Instant.now();

        // Counted for every email, before checking access, so a 429 does not reveal
        // whether the email is allowed (CON-09, scenario 2).
        if (!otpChallengeStore.registerRequest(
                email.value(),
                otpMaxRequests,
                Duration.ofMinutes(otpRequestWindowMinutes),
                now
        )) {
            throw new OtpRequestLimitExceededException();
        }

        // Avoid revealing whether an account exists.
        if (!isEmailAllowed(email)) {
            return;
        }

        if (otpExpirationMinutes <= 0) {
            throw new IllegalStateException(
                    "Invalid OTP expiration configuration."
            );
        }

        String otpCode = otpCodeGenerator.generate();

        if (otpCode == null ||
                !otpCode.matches("^[0-9]{6}$")) {
            throw new IllegalStateException(
                    "OTP generator returned an invalid code."
            );
        }

        String otpHash = tokenHashingService.hash(
                "OTP:" + email.value(),
                otpCode
        );

        boolean issued = otpChallengeStore.issue(
                email.value(),
                otpHash,
                now.plus(otpExpirationMinutes, ChronoUnit.MINUTES),
                now
        );

        if (issued) {
            otpDeliveryService.send(email.value(), otpCode);
        }
    }

    @Override
    @Transactional
    public AuthenticationResult handle(SignInCommand command) {
        EmailAddress email = new EmailAddress(command.email());

        if (command.otpCode() == null ||
                !command.otpCode().matches("^[0-9]{6}$")) {
            throw new BadCredentialsException(
                    "Invalid or expired access code."
            );
        }

        Instant now = Instant.now();

        String otpHash = tokenHashingService.hash(
                "OTP:" + email.value(),
                command.otpCode()
        );

        boolean validOtp = otpChallengeStore.consume(
                email.value(),
                otpHash,
                now
        );

        if (!validOtp) {
            throw new BadCredentialsException(
                    "Invalid or expired access code."
            );
        }

        User user = userPersistencePort
                .findByEmail(email.value())
                .orElseGet(() -> registerVerifiedDriver(email, now));

        validateAccountAccess(user);

        user.registerSuccessfulLogin(now);

        User savedUser = userPersistencePort.save(user);

        return createSession(savedUser, now);
    }

    @Override
    // The family revocation of a reused token must survive the 401 it causes.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthenticationResult handle(
            RefreshSessionCommand command
    ) {
        if (command.refreshToken() == null ||
                command.refreshToken().isBlank()) {
            throw new BadCredentialsException(
                    "Invalid refresh token."
            );
        }

        Instant now = Instant.now();

        String tokenHash = tokenHashingService.hash(
                "REFRESH_TOKEN",
                command.refreshToken()
        );

        RefreshToken currentToken = refreshTokenPersistencePort
                .findForUpdateByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid refresh token."
                ));

        if (currentToken.getRevokedAt() != null) {
            // A rotated or signed-out token was presented again: someone else may hold the
            // current token of this session, so the whole family is revoked.
            int revoked = refreshTokenPersistencePort.revokeFamily(currentToken.getFamilyId(), now);
            if (revoked > 0) {
                log.warn("Refresh token reuse detected for user {}: revoked {} active tokens of its session",
                        currentToken.getUserId(), revoked);
            }
            throw new BadCredentialsException(
                    "Refresh token is expired or revoked."
            );
        }

        if (!currentToken.isValidAt(now)) {
            throw new BadCredentialsException(
                    "Refresh token is expired or revoked."
            );
        }

        User user = userPersistencePort
                .findById(currentToken.getUserId())
                .orElseThrow(() -> new BadCredentialsException(
                        "Invalid refresh token."
                ));

        validateAccountAccess(user);

        // Revoke the previous refresh token.
        currentToken.revoke(now);
        refreshTokenPersistencePort.save(currentToken);

        // Rotate within the same session (family).
        return createSession(user, now, currentToken.getFamilyId());
    }

    @Override
    @Transactional
    public void handle(SignOutCommand command) {
        if (command.refreshToken() == null ||
                command.refreshToken().isBlank()) {
            return;
        }

        String tokenHash = tokenHashingService.hash(
                "REFRESH_TOKEN",
                command.refreshToken()
        );

        Optional<RefreshToken> token = refreshTokenPersistencePort
                .findForUpdateByTokenHash(tokenHash);

        if (token.isPresent() &&
                token.get().getRevokedAt() == null) {

            token.get().revoke(Instant.now());

            refreshTokenPersistencePort.save(token.get());
        }
    }

    private boolean isEmailAllowed(EmailAddress email) {
        Optional<User> existingUser =
                userPersistencePort.findByEmail(email.value());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            return user.getStatus() == UserStatus.ACTIVE &&
                    hasValidInstitutionalAccess(user);
        }

        return findInstitutionForEmail(email).isPresent();
    }

    private Optional<Long> findInstitutionForEmail(
            EmailAddress email
    ) {
        return institutionDomainPersistencePort
                .findVerifiedByDomain(email.domain())
                .flatMap(domain ->
                        institutionPersistencePort
                                .findById(domain.getInstitutionId())
                                .filter(institution -> institution.isActive())
                                .map(institution -> institution.getId())
                );
    }

    private User registerVerifiedDriver(
            EmailAddress email,
            Instant now
    ) {
        Long institutionId = findInstitutionForEmail(email)
                .orElseThrow(() -> new BadCredentialsException(
                        "Email domain is not authorized."
                ));

        User newDriver = User.registerDriver(
                institutionId,
                email.value(),
                now
        );

        return userPersistencePort.save(newDriver);
    }

    private void validateAccountAccess(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "Account is not active."
            );
        }

        if (!hasValidInstitutionalAccess(user)) {
            throw new BadCredentialsException(
                    "Institutional access is not authorized."
            );
        }
    }

    private boolean hasValidInstitutionalAccess(User user) {
        if (user.getRole() == Roles.PLATFORM_ADMIN) {
            return user.getInstitutionId() == null;
        }

        Optional<Long> verifiedInstitutionId =
                findInstitutionForEmail(user.getEmail());

        return verifiedInstitutionId.isPresent() &&
                verifiedInstitutionId.get()
                        .equals(user.getInstitutionId());
    }

    private AuthenticationResult createSession(
            User user,
            Instant now
    ) {
        return createSession(user, now, UUID.randomUUID());
    }

    private AuthenticationResult createSession(
            User user,
            Instant now,
            UUID familyId
    ) {
        if (user.getId() == null) {
            throw new IllegalStateException(
                    "Cannot issue tokens for an unsaved user."
            );
        }

        if (refreshExpirationDays <= 0) {
            throw new IllegalStateException(
                    "Invalid refresh token expiration configuration."
            );
        }

        var issuedAccessToken = accessTokenService.issue(
                user,
                now
        );

        if (issuedAccessToken == null ||
                issuedAccessToken.value() == null ||
                issuedAccessToken.value().isBlank() ||
                issuedAccessToken.expiresAt() == null ||
                !issuedAccessToken.expiresAt().isAfter(now)) {
            throw new IllegalStateException(
                    "Invalid access token response."
            );
        }

        String rawRefreshToken = secureTokenGenerator.generate();

        String refreshHash = tokenHashingService.hash(
                "REFRESH_TOKEN",
                rawRefreshToken
        );

        RefreshToken refreshToken = RefreshToken.issue(
                user.getId(),
                familyId,
                refreshHash,
                now.plus(refreshExpirationDays, ChronoUnit.DAYS),
                now
        );

        refreshTokenPersistencePort.save(refreshToken);

        return new AuthenticationResult(
                user,
                issuedAccessToken.value(),
                rawRefreshToken,
                issuedAccessToken.expiresAt()
        );
    }
}

