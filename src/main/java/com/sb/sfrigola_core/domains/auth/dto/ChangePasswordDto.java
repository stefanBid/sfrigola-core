package com.sb.sfrigola_core.domains.auth.dto;

import com.sb.sfrigola_core.common.annotations.validations.password.PasswordConstants;
import com.sb.sfrigola_core.common.annotations.validations.password.ValidPassword;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

public record ChangePasswordDto(
        @NotBlank(message = PasswordConstants.PASSWORD_REQUIRED)
        String oldPassword,

        @ValidPassword
        String newPassword,

        @NotBlank(message = PasswordConstants.PASSWORD_REQUIRED)
        String confirmNewPassword
) implements Serializable {
}
