package com.knowflow.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 128) String nickname,
        @Size(max = 128) String email,
        @Size(max = 512) String avatar,
        @Size(max = 512) String bio
) {
}
