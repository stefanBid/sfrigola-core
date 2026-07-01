package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import org.springframework.http.HttpStatus;

public class TagSlugAlreadyExistsException extends SCGeneralException {
    public TagSlugAlreadyExistsException(String slug) {
        super(
                HttpStatus.CONFLICT,
                TagErrorCode.TAG_SLUG_ALREADY_EXISTS,
                "A tag with slug '" + slug + "' already exists"
        );
    }
}