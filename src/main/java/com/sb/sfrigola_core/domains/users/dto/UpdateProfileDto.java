package com.sb.sfrigola_core.domains.users.dto;

import com.sb.sfrigola_core.domains.users.constants.UserValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileDto(
        @NotBlank(message = UserValidationCodeConstants.FIRST_NAME_IS_REQUIRED)
        @Size(max = 100, message = UserValidationCodeConstants.FIRST_NAME_TOO_LONG)
        String firstName,

        @NotBlank(message = UserValidationCodeConstants.LAST_NAME_IS_REQUIRED)
        @Size(max = 100, message = UserValidationCodeConstants.LAST_NAME_TOO_LONG)
        String lastName,

        @Size(max = 1000, message = UserValidationCodeConstants.BIO_TOO_LONG)
        String bio
) {
}
