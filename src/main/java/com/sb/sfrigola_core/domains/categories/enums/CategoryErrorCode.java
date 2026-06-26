package com.sb.sfrigola_core.domains.categories.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum CategoryErrorCode implements ISCErrorCode {

    SELECTED_CATEGORY_NOT_FOUND,
    CATEGORY_SLUG_ALREADY_EXISTS,
    INVALID_CATEGORY_LOCALE,
    DUPLICATE_CATEGORY_LOCALE;

    @Override
    public String code() {
        return name();
    }
}
