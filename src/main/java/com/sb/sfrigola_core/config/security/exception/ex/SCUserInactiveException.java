package com.sb.sfrigola_core.config.security.exception.ex;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.config.security.exception.SecurityErrorCode;
import org.springframework.http.HttpStatus;

public class SCUserInactiveException extends SCGeneralException {
    public SCUserInactiveException(String message) {
        super(
                HttpStatus.FORBIDDEN,
                SecurityErrorCode.USER_NOT_ACTIVE,
                message
        );
    }
}
