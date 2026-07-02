package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateTagLocaleException extends SCGeneralException {
    public DuplicateTagLocaleException(String locale) {
        super(
                HttpStatus.BAD_REQUEST,
                TagErrorCode.DUPLICATE_TAG_LOCALE,
                "Locale '" + locale + "' appears more than once in the translations list"
        );
    }
}
