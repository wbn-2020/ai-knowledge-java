package com.knowflow.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameSessionRequest(@NotBlank String title) {
}
