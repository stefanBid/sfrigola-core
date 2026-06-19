package com.sb.sfrigola_core.domains.auth.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.config.security.exception.SecurityErrorCode;
import org.springframework.http.HttpStatus;

public class SCAuthSecuritySystemException extends SCGeneralException {
    public SCAuthSecuritySystemException(String message) {
        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                SecurityErrorCode.SECURITY_SYSTEM_ERROR,
                message);
    }
}
