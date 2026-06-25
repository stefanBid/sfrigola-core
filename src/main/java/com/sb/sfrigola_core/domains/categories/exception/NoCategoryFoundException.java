package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

public class NoCategoryFoundException extends SCGeneralException {
    public NoCategoryFoundException(String publicId) {
        super(
                HttpStatus.NOT_FOUND,
                CategoryErrorCode.SELECTED_CATEGORY_NOT_FOUND,
                "No category found with ID: " + publicId
        );

    }
}
