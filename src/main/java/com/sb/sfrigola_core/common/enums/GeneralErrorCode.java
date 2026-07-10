package com.sb.sfrigola_core.common.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum GeneralErrorCode implements ISCErrorCode {
    DATA_CORRUPTED,
    NO_ROWS_AFFECTED,
    ENTITY_NOT_FOUND,
    ENV_NOT_AVAILABLE,
    ILLEGAL_ARGUMENT,
    INVALID_ENUM_CODE,
    MALFORMED_JSON,
    VALIDATION_FAILED,
    SERVER_ERROR,
    LOCALE_NOT_ACTIVE;


    @Override
    public String code() {
        return name();
    }
}
