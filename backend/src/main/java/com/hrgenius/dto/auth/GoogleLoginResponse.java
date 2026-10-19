package com.hrgenius.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result of "Sign in with Google": either a session (status SIGNED_IN) or a sign-up request
 * waiting for administrator approval (status PENDING_APPROVAL).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleLoginResponse(String status, String message, AuthResponse auth) {

    public static GoogleLoginResponse signedIn(AuthResponse auth) {
        return new GoogleLoginResponse("SIGNED_IN", null, auth);
    }

    public static GoogleLoginResponse pending(String message) {
        return new GoogleLoginResponse("PENDING_APPROVAL", message, null);
    }
}
