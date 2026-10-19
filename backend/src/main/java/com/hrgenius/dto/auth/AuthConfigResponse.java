package com.hrgenius.dto.auth;

/** Public settings the login page needs before anyone is signed in. */
public record AuthConfigResponse(boolean signupEnabled, boolean googleEnabled, String googleClientId) {
}
