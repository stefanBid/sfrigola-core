package com.sb.sfrigola_core.common.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum GeneralErrorCode implements ISCErrorCode {
    DATA_CORRUPTED,
    ENTITY_NOT_FOUND,
    ENV_NOT_AVAILABLE,
    ILLEGAL_ARGUMENT,
    MALFORMED_JSON,
    VALIDATION_FAILED,
    SERVER_ERROR;


    @Override
    public String code() {
        return name();
    }
}
