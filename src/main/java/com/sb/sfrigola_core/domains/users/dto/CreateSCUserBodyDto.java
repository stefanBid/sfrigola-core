package com.sb.sfrigola_core.domains.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record CreateSCUserBodyDto(
        @NotBlank
        @Size(max = 50)
        String username,

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank()
        @Size(min = 8, max = 255)
        String password,

        @NotBlank
        @Size(max = 10)
        String preferredLang,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName
) implements Serializable {
}
