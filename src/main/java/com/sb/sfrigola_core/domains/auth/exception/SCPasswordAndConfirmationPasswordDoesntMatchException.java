package com.sb.sfrigola_core.domains.auth.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.auth.enums.AuthErrorCode;
import org.springframework.http.HttpStatus;

public class SCPasswordAndConfirmationPasswordDoesntMatchException extends SCGeneralException {
    public SCPasswordAndConfirmationPasswordDoesntMatchException(String message) {

        super(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.PASSWORD_DOES_NOT_MATCH_CONFIRMATION_PASSWORD,
                message
        );
    }
}
