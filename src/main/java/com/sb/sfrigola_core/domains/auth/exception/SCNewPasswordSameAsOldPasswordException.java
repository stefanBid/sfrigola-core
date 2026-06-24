package com.sb.sfrigola_core.domains.auth.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.auth.enums.AuthErrorCode;
import org.springframework.http.HttpStatus;

public class SCNewPasswordSameAsOldPasswordException extends SCGeneralException {
    public SCNewPasswordSameAsOldPasswordException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.NEW_PASSWORD_SAME_AS_OLD_PASSWORD,
                message
        );
    }
}
