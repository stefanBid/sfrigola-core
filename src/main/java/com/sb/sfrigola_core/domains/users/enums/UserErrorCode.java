package com.sb.sfrigola_core.domains.users.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum UserErrorCode implements ISCErrorCode {
    INVALID_ROLE_FROM_STRING,
    INVALID_LANG_CODE;


    @Override
    public String code() {
        return name();
    }
}
