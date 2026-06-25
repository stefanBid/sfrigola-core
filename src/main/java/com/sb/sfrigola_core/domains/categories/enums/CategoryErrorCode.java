package com.sb.sfrigola_core.domains.categories.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum CategoryErrorCode implements ISCErrorCode {

    SELECTED_CATEGORY_NOT_FOUND;

    @Override
    public String code() {
        return name();
    }
}
