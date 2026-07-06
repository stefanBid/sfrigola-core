package com.sb.sfrigola_core.domains.tags.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum TagErrorCode implements ISCErrorCode {

    TAG_NOT_FOUND,
    TAG_SLUG_ALREADY_EXISTS,
    TAG_LABEL_ALREADY_EXISTS,
    TAG_LANGUAGE_NOT_ACTIVE,
    DUPLICATE_TAG_LOCALE,
    TAG_SCOPE_NOT_ALLOWED;

    @Override
    public String code() {
        return name();
    }
}