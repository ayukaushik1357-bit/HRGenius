package com.hrgenius.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result of creating an account or signing in with Google: either a session (status SIGNED_IN)
 * or, when approval is switched on, a request waiting for an administrator (status PENDING_APPROVAL).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignInResult(String status, String message, AuthResponse auth) {

    public static SignInResult signedIn(AuthResponse auth) {
        return new SignInResult("SIGNED_IN", null, auth);
    }

    public static SignInResult pending(String message) {
        return new SignInResult("PENDING_APPROVAL", message, null);
    }
}
