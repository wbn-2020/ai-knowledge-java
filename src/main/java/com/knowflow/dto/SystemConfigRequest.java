package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;

public record SystemConfigRequest(@NotBlank String configKey, @NotBlank String configValue, String description) {
}
