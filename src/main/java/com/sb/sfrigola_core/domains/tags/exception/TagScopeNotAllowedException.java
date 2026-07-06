package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TagScopeNotAllowedException extends SCGeneralException {
    public TagScopeNotAllowedException(UUID publicId, TagScope actualScope) {
        super(
                HttpStatus.BAD_REQUEST,
                TagErrorCode.TAG_SCOPE_NOT_ALLOWED,
                "Tag with ID: " + publicId + " has scope '" + actualScope.getValue() + "' and cannot be used in this context"
        );
    }
}