package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

public class CategorySlugAlreadyExistsException extends SCGeneralException {
    public CategorySlugAlreadyExistsException(String slug) {
        super(
                HttpStatus.CONFLICT,
                CategoryErrorCode.CATEGORY_SLUG_ALREADY_EXISTS,
                "A category with slug '" + slug + "' already exists"
        );
    }
}