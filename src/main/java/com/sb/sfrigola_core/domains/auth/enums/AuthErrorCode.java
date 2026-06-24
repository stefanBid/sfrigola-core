package com.sb.sfrigola_core.domains.auth.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum AuthErrorCode implements ISCErrorCode {
    COMPROMISED_PASSWORD,
    PASSWORD_DOES_NOT_MATCH_CONFIRMATION_PASSWORD,
    USER_ALREADY_EXISTS,
    OLD_PASSWORD_NOT_MATCH,
    NEW_PASSWORD_SAME_AS_OLD_PASSWORD;

    @Override
    public String code() {
        return name();
    }
}