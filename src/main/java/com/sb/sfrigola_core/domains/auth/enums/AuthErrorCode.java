package com.sb.sfrigola_core.domains.auth.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum AuthErrorCode implements ISCErrorCode {
    USER_ALREADY_EXISTS,
    COMPROMISED_PASSWORD;

    @Override
    public String code() {
        return name();
    }
}