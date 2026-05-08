package com.knowflow.modules.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameDocumentRequest(@NotBlank @Size(max = 255) String name) {
}
