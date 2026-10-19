package com.hrgenius.security;

/** Verifies a Google Identity Services ID token ("credential") and returns the verified identity. */
public interface GoogleTokenVerifier {

    GoogleIdentity verify(String idToken);

    record GoogleIdentity(String email, String givenName, String familyName) {
    }
}
