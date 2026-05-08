package com.knowflow.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameSessionRequest(@NotBlank String title) {
}
