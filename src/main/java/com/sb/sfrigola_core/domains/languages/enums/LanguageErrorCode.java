package com.sb.sfrigola_core.domains.languages.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum LanguageErrorCode implements ISCErrorCode {
    INVALID_LANG_CODE;

    @Override
    public String code() {
        return name();
    }
}
