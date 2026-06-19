package com.sb.sfrigola_core.config.security.exception.ex;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.config.security.exception.SecurityErrorCode;
import org.springframework.http.HttpStatus;

public class SCBadCredentialException extends SCGeneralException {
    public SCBadCredentialException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                SecurityErrorCode.BAD_CREDENTIALS,
                message);
    }
}
