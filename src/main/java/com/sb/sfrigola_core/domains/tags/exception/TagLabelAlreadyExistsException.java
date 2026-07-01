package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import org.springframework.http.HttpStatus;

public class TagLabelAlreadyExistsException extends SCGeneralException {
    public TagLabelAlreadyExistsException(String label, String locale) {
        super(
                HttpStatus.CONFLICT,
                TagErrorCode.TAG_LABEL_ALREADY_EXISTS,
                "A tag with label '" + label + "' already exists in language '" + locale + "'"
        );
    }
}