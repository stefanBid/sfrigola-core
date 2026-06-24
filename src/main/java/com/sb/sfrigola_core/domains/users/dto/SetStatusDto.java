package com.sb.sfrigola_core.domains.users.dto;

import com.sb.sfrigola_core.domains.users.constants.UserValidationCodeConstants;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record SetStatusDto(
        @NotNull(message = UserValidationCodeConstants.IS_ACTIVE_IS_REQUIRED)
        Boolean active
) implements Serializable {
}
