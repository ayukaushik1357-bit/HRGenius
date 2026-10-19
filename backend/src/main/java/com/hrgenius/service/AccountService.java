package com.hrgenius.service;

import com.hrgenius.config.AccountProperties;
import com.hrgenius.dto.auth.AuthConfigResponse;
import com.hrgenius.dto.auth.SignInResult;
import com.hrgenius.dto.auth.PasswordResetConfirmRequest;
import com.hrgenius.dto.auth.RegisterRequest;
import com.hrgenius.entity.PasswordResetToken;
import com.hrgenius.entity.Role;
import com.hrgenius.entity.User;
import com.hrgenius.entity.notification.AppNotification;
import com.hrgenius.exception.AccountPendingApprovalException;
import com.hrgenius.exception.BadRequestException;
import com.hrgenius.exception.DuplicateResourceException;
import com.hrgenius.exception.ResourceNotFoundException;
import com.hrgenius.repository.PasswordResetTokenRepository;
import com.hrgenius.repository.RefreshTokenRepository;
import com.hrgenius.repository.RoleRepository;
import com.hrgenius.repository.UserRepository;
import com.hrgenius.security.GoogleTokenVerifier;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Account self-service: sign-up (instant by default, optional administrator approval),
 * password reset by email, and "Sign in with Google".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    public static final String PENDING_MESSAGE =
            "Thanks! Your account request has been sent. An administrator must approve it before you can sign in.";
    public static final String RESET_REQUESTED_MESSAGE =
            "If an account exists for that email, a password reset link has been sent.";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AuthService authService;
    private final AccountProperties props;
    private final EmployeeService employeeService;

    public AuthConfigResponse publicConfig() {
        return new AuthConfigResponse(props.isSignupEnabled(), props.isGoogleEnabled(),
                props.isGoogleEnabled() ? props.getGoogleClientId() : null);
    }

    // ------------------------------------------------------------------ sign-up

    @Transactional
    public SignInResult register(RegisterRequest request, HttpServletResponse response) {
        if (!props.isSignupEnabled()) {
            throw new BadRequestException("Self sign-up is disabled. Please contact HR.");
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!props.isDomainAllowed(email)) {
            throw new BadRequestException("Please sign up with your company email address.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists. Try signing in or resetting your password.");
        }
        AuthService.validatePasswordStrength(request.getPassword());

        User user = createAccount(email, request.getFirstName(), request.getLastName(),
                passwordEncoder.encode(request.getPassword()));
        return user.isEnabled()
                ? SignInResult.signedIn(authService.issueSession(user, response))
                : SignInResult.pending(PENDING_MESSAGE);
    }

    /**
     * Creates an EMPLOYEE account. By default it is active straight away and gets its own employee
     * profile; with app.account.signup-requires-approval=true it waits for an administrator instead.
     */
    private User createAccount(String email, String firstName, String lastName, String passwordHash) {
        if (props.isSignupRequiresApproval()) {
            return createPendingUser(email, firstName, lastName, passwordHash);
        }
        Role employeeRole = roleRepository.findByName(Role.RoleName.EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.RoleName.EMPLOYEE));
        User user = new User();
        user.setUsername(uniqueUsernameFor(email));
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setEnabled(true);
        user.setPendingApproval(false);
        user.setMustChangePassword(false);
        user.setRoles(Collections.singleton(employeeRole));
        User saved = userRepository.save(user);
        employeeService.createSelfServiceProfile(saved, firstName, lastName);
        log.info("New account '{}' created by self sign-up", saved.getUsername());
        return saved;
    }

    private User createPendingUser(String email, String firstName, String lastName, String passwordHash) {
        Role employeeRole = roleRepository.findByName(Role.RoleName.EMPLOYEE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.RoleName.EMPLOYEE));

        User user = new User();
        user.setUsername(uniqueUsernameFor(email));
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setEnabled(false);
        user.setPendingApproval(true);
        user.setMustChangePassword(false);
        user.setRoles(Collections.singleton(employeeRole));
        User saved = userRepository.save(user);

        String who = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        for (User admin : userRepository.findActiveByRole(Role.RoleName.ADMIN)) {
            notificationService.sendInAppNotification(admin, "New sign-up request",
                    (who.isEmpty() ? email : who + " (" + email + ")") + " is waiting for approval.",
                    AppNotification.NotificationType.GENERAL, "/users");
        }
        log.info("New account '{}' registered and waiting for approval", saved.getUsername());
        return saved;
    }

    private String uniqueUsernameFor(String email) {
        String base = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
        if (base.length() < 3) {
            base = "user" + base;
        }
        base = base.length() > 40 ? base.substring(0, 40) : base;
        String candidate = base;
        int n = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + (++n);
        }
        return candidate;
    }

    // ---------------------------------------------------------- password reset

    @Transactional
    public String requestPasswordReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        // Same answer whether or not the account exists, so the form cannot be used to discover emails
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            return RESET_REQUESTED_MESSAGE;
        }
        User user = userOpt.get();
        resetTokenRepository.invalidateAllForUser(user);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken entity = new PasswordResetToken();
        entity.setUser(user);
        entity.setTokenHash(AuthService.hashToken(token));
        entity.setExpiresAt(LocalDateTime.now().plusMinutes(props.getPasswordResetTtlMinutes()));
        entity.setCreatedAt(LocalDateTime.now());
        resetTokenRepository.save(entity);

        String link = props.getFrontendUrl().replaceAll("/+$", "") + "/auth/reset-password?token=" + token;
        notificationService.sendEmailNotification(user.getEmail(), "Reset your HRGenius password",
                "Hi " + user.getUsername() + ",\n\n"
                        + "We received a request to reset your HRGenius password. Open this link to choose a new one:\n\n"
                        + link + "\n\n"
                        + "The link works once and expires in " + props.getPasswordResetTtlMinutes() + " minutes.\n"
                        + "If you did not ask for this, you can ignore this email.");
        if (props.isLogPasswordResetLink()) {
            log.info("Password reset link for '{}' (dev only, disable in prod): {}", user.getUsername(), link);
        }
        return RESET_REQUESTED_MESSAGE;
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(AuthService.hashToken(request.getToken()))
                .filter(t -> !t.isUsed() && !t.isExpired())
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired. Please request a new one."));

        AuthService.validatePasswordStrength(request.getNewPassword());

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setFailedAttempts(0);
        user.setLockoutTime(null);
        userRepository.save(user);

        token.setUsed(true);
        resetTokenRepository.save(token);
        // Signing in elsewhere with the old password is no longer possible; end those sessions too
        refreshTokenRepository.revokeAllByUser(user);
    }

    // ---------------------------------------------------------- Google sign-in

    @Transactional
    public SignInResult googleLogin(String credential, HttpServletResponse response) {
        GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(credential);
        String email = identity.email().trim().toLowerCase(Locale.ROOT);

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.isPendingApproval()) {
                throw new AccountPendingApprovalException();
            }
            if (!user.isEnabled()) {
                throw new DisabledException("Account is disabled");
            }
            return SignInResult.signedIn(authService.issueSession(user, response));
        }

        // Unknown Google account: create it (Google has already verified the email)
        if (!props.isSignupEnabled()) {
            throw new BadRequestException("No HRGenius account is linked to " + email + ". Please contact HR.");
        }
        if (!props.isDomainAllowed(email)) {
            throw new BadRequestException("Please use your company Google account.");
        }
        // Random password nobody knows; the user can set one later with "Forgot password"
        User user = createAccount(email, identity.givenName(), identity.familyName(),
                passwordEncoder.encode(UUID.randomUUID() + "Aa1!"));
        return user.isEnabled()
                ? SignInResult.signedIn(authService.issueSession(user, response))
                : SignInResult.pending(PENDING_MESSAGE);
    }
}
