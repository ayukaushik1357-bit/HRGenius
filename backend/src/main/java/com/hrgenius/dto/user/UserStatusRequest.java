package com.hrgenius.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusRequest {

    @NotNull(message = "Enabled flag is required")
    private Boolean enabled;
}
