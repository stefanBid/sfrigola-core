package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class NoValidRoleFromExternalException extends SCGeneralException {
    public NoValidRoleFromExternalException(String message) {
        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                UserErrorCode.INVALID_ROLE_FROM_STRING,
                message
        );
    }
}
