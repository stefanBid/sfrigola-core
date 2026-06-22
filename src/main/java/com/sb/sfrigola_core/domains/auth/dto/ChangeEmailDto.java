package com.sb.sfrigola_core.domains.auth.dto;

import com.sb.sfrigola_core.domains.users.constants.UserValidationCodeConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record ChangeEmailDto(
        @NotBlank(message = UserValidationCodeConstants.EMAIL_REQUIRED)
        @Email(message = UserValidationCodeConstants.INVALID_EMAIL_FORMAT)
        @Size(max = 150, message = UserValidationCodeConstants.EMAIL_TOO_LONG)
        String newEmail
) implements Serializable {
}
