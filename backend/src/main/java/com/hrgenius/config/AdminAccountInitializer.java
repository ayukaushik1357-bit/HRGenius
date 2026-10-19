package com.hrgenius.config;

import com.hrgenius.repository.UserRepository;
import com.hrgenius.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Sets the password of the built-in "admin" account from ADMIN_PASSWORD (app.account.admin-password),
 * so the admin password is never a public default. Runs after the demo-data seeder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountInitializer {

    private final AccountProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // No @Transactional here: each repository call runs in its own transaction, so a missing table
    // (e.g. an empty test database) is logged instead of failing application startup.
    @EventListener(ApplicationReadyEvent.class)
    public void applyAdminPassword() {
        String password = props.getAdminPassword();
        java.util.Optional<com.hrgenius.entity.User> adminAccount;
        try {
            adminAccount = userRepository.findByUsername("admin");
        } catch (org.springframework.dao.DataAccessException ex) {
            log.warn("Could not read the 'admin' account ({}); ADMIN_PASSWORD was not applied.", ex.getMostSpecificCause().getMessage());
            return;
        }
        adminAccount.ifPresentOrElse(admin -> {
            if (password == null || password.isBlank()) {
                log.warn("ADMIN_PASSWORD is not set. Set it to choose the password of the 'admin' account.");
                return;
            }
            if (passwordEncoder.matches(password, admin.getPassword())) {
                return; // already applied
            }
            AuthService.validatePasswordStrength(password);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setMustChangePassword(false);
            admin.setFailedAttempts(0);
            admin.setLockoutTime(null);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Password of the 'admin' account set from ADMIN_PASSWORD.");
        }, () -> log.warn("No 'admin' account found; ADMIN_PASSWORD was not applied."));
    }
}
