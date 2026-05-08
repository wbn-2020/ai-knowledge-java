package com.knowflow.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String oldPassword,
        @NotBlank @Size(min = 6, max = 64) String newPassword,
        @NotBlank @Size(min = 6, max = 64) String confirmPassword
) {
}
