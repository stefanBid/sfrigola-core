package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import org.springframework.http.HttpStatus;

public class MissingTagLocalesException extends SCGeneralException {
    public MissingTagLocalesException() {
        super(
                HttpStatus.BAD_REQUEST,
                TagErrorCode.MISSING_TAG_LOCALES,
                "Translations must cover all active languages"
        );
    }
}
