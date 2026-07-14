package com.sb.sfrigola_core.domains.favorites.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum FavoriteErrorCode implements ISCErrorCode {

    FAVORITE_NOT_FOUND,
    FAVORITE_ALREADY_EXISTS;

    @Override
    public String code() {
        return name();
    }
}
