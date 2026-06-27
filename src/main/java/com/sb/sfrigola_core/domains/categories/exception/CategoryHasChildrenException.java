package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class CategoryHasChildrenException extends SCGeneralException {
    public CategoryHasChildrenException(UUID publicId) {

        super(
                HttpStatus.BAD_REQUEST,
                CategoryErrorCode.CATEGORY_HAS_CHILDREN,
                "Category with " + publicId + " has child categories and cannot be deleted."
        );

    }
}
