package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import org.springframework.http.HttpStatus;

public class TagLanguageNotActiveException extends SCGeneralException {
    public TagLanguageNotActiveException(String langCode) {
        super(
                HttpStatus.BAD_REQUEST,
                TagErrorCode.TAG_LANGUAGE_NOT_ACTIVE,
                "Language '" + langCode + "' is not active in the system"
        );
    }
}