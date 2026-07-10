package com.sb.sfrigola_core.domains.ratings.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum RatingErrorCode implements ISCErrorCode {

    RATING_NOT_FOUND,
    RATING_ALREADY_EXISTS;

    @Override
    public String code() {
        return name();
    }
}
