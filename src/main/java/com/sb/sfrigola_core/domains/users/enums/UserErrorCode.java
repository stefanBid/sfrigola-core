package com.sb.sfrigola_core.domains.users.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum UserErrorCode implements ISCErrorCode {
    INVALID_ROLE_FROM_STRING,
    CANNOT_CHANGE_OWN_ACTIVE_STATUS,
    CANNOT_CHANGE_ROLE_TO_ADMIN,
    USER_NOT_FOUND,
    INVALID_DEFAULT_AVATAR;


    @Override
    public String code() {
        return name();
    }
}
