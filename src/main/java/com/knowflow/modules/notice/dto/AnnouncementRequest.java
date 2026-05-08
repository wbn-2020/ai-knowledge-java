package com.knowflow.modules.notice.dto;

import jakarta.validation.constraints.NotBlank;

public record AnnouncementRequest(@NotBlank String title, @NotBlank String content, Boolean enabled) {
}
