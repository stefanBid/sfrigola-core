package com.sb.sfrigola_core.domains.auth.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.auth.enums.AuthErrorCode;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class SCUserAlreadyExistsException extends SCGeneralException {
    public SCUserAlreadyExistsException(String message) {
        super(
                HttpStatus.CONFLICT,
                AuthErrorCode.USER_ALREADY_EXISTS,
                message
        );
    }
}