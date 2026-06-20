package com.sb.sfrigola_core.domains.users.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileDto(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl,

        @Size(max = 1000, message = "Bio must not exceed 1000 characters")
        String bio
) {
}
