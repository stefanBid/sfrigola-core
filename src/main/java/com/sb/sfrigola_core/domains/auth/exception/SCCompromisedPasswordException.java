package com.sb.sfrigola_core.domains.auth.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.auth.enums.AuthErrorCode;
import org.springframework.http.HttpStatus;

public class SCCompromisedPasswordException extends SCGeneralException {
    public SCCompromisedPasswordException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                AuthErrorCode.COMPROMISED_PASSWORD,
                message
        );
    }
}