package com.sb.sfrigola_core.domains.tags.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.tags.enums.TagErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoTagFoundException extends SCGeneralException {
    public NoTagFoundException(UUID publicId) {
        super(
                HttpStatus.NOT_FOUND,
                TagErrorCode.TAG_NOT_FOUND,
                "No tag found with ID: " + publicId
        );
    }
}