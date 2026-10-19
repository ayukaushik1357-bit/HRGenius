package com.hrgenius.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Settings for sign-up, password reset and Google sign-in ("app.account.*"). */
@Component
@ConfigurationProperties(prefix = "app.account")
@Getter
@Setter
public class AccountProperties {

    /** Public URL of the Angular app, used to build password reset links. */
    private String frontendUrl = "http://localhost:4200";

    /** Allow people to request an account themselves (still needs admin approval). */
    private boolean signupEnabled = true;

    /** If not empty, only these email domains may sign up (e.g. "company.com"). */
    private List<String> signupAllowedDomains = new ArrayList<>();

    /** Minutes a password reset link stays valid. */
    private int passwordResetTtlMinutes = 30;

    /** Also write the reset link to the server log (handy for local demos without SMTP; keep false in prod). */
    private boolean logPasswordResetLink = false;

    /** OAuth 2.0 Web client ID from Google Cloud Console. Empty = Google sign-in disabled. */
    private String googleClientId = "";

    public boolean isGoogleEnabled() {
        return googleClientId != null && !googleClientId.isBlank();
    }

    public boolean isDomainAllowed(String email) {
        if (signupAllowedDomains == null || signupAllowedDomains.isEmpty()) {
            return true;
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return signupAllowedDomains.stream().anyMatch(d -> d.trim().equalsIgnoreCase(domain));
    }
}
