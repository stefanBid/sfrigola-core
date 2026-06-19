package com.sb.sfrigola_core.config.security.exception.ex;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.config.security.exception.SecurityErrorCode;
import org.springframework.http.HttpStatus;

public class SCAuthenticatedUserNotFoundException extends SCGeneralException {
    public SCAuthenticatedUserNotFoundException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                SecurityErrorCode.NO_USER_AUTH,
                message);
    }
}
