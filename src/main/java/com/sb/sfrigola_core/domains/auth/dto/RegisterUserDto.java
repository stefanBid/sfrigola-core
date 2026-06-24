package com.sb.sfrigola_core.domains.auth.dto;

import com.sb.sfrigola_core.domains.auth.annotations.validations.password.ValidPassword;
import com.sb.sfrigola_core.domains.users.constants.UserValidationCodeConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record RegisterUserDto(
        @NotBlank(message = UserValidationCodeConstants.USERNAME_REQUIRED)
        @Size(max = 50, message = UserValidationCodeConstants.USERNAME_TOO_LONG)
        @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = UserValidationCodeConstants.USERNAME_INVALID_FORMAT)
        String username,

        @NotBlank(message = UserValidationCodeConstants.EMAIL_REQUIRED)
        @Email(message = UserValidationCodeConstants.INVALID_EMAIL_FORMAT)
        @Size(max = 150, message = UserValidationCodeConstants.EMAIL_TOO_LONG)
        String email,

        @ValidPassword
        String password,

        String confirmPassword,

        @NotBlank(message = UserValidationCodeConstants.PREFERRED_LANG_REQUIRED)
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = UserValidationCodeConstants.PREFERRED_LANG_INVALID_FORMAT)
        String preferredLang,

        @NotBlank(message = UserValidationCodeConstants.FIRST_NAME_IS_REQUIRED)
        @Size(max = 100, message = UserValidationCodeConstants.FIRST_NAME_TOO_LONG)
        String firstName,

        @NotBlank(message = UserValidationCodeConstants.LAST_NAME_IS_REQUIRED)
        @Size(max = 100, message = UserValidationCodeConstants.LAST_NAME_TOO_LONG)
        String lastName
) implements Serializable {
}
