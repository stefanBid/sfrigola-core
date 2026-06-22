package com.sb.sfrigola_core.domains.languages.enums;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;

public enum LanguageErrorCode implements ISCErrorCode {
    INVALID_LANG_CODE;

    @Override
    public String code() {
        return name();
    }
}
