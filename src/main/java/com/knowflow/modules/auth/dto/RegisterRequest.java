package com.knowflow.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 64) String username,
        @Size(max = 128) String email,
        @NotBlank @Size(min = 6, max = 64) String password,
        @NotBlank @Size(min = 6, max = 64) String confirmPassword
) {
}
