package com.hrgenius.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {

    /** The ID token ("credential") returned by Google Identity Services in the browser. */
    @NotBlank(message = "Google credential is required")
    private String credential;
}
