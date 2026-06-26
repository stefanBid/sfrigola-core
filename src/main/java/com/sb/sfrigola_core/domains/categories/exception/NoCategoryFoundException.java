package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoCategoryFoundException extends SCGeneralException {
    public NoCategoryFoundException(UUID publicId) {
        super(
                HttpStatus.NOT_FOUND,
                CategoryErrorCode.SELECTED_CATEGORY_NOT_FOUND,
                "No category found with ID: " + publicId
        );

    }
}
