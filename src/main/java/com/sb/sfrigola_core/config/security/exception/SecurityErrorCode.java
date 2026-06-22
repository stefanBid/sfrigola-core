package com.sb.sfrigola_core.config.security.exception;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum SecurityErrorCode implements ISCErrorCode {
    NOT_AUTHORIZED,
    USER_NOT_FOUND,
    USER_NOT_ACTIVE,
    NO_USER_AUTH,
    BAD_CREDENTIALS,
    JWT_INVALID_FORMAT,
    JWT_EXPIRED,
    JWT_VALIDATION_FAILED,
    JWT_NO_CLAIM_PARAM,
    SECURITY_SYSTEM_ERROR;

    @Override
    public String code() {
        return name();
    }
}
