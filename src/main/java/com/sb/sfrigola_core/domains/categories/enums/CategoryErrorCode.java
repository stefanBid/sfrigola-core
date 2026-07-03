package com.sb.sfrigola_core.domains.categories.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum CategoryErrorCode implements ISCErrorCode {

    SELECTED_CATEGORY_NOT_FOUND,
    CATEGORY_SLUG_ALREADY_EXISTS,
    CATEGORY_HAS_CHILDREN,
    INVALID_CATEGORY_LOCALE,
    DUPLICATE_CATEGORY_LOCALE,
    CATEGORY_REORDER_MISMATCH;

    @Override
    public String code() {
        return name();
    }
}
