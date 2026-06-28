package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

public class CategoryReorderMismatchException extends SCGeneralException {
    public CategoryReorderMismatchException() {
        super(
                HttpStatus.BAD_REQUEST,
                CategoryErrorCode.CATEGORY_REORDER_MISMATCH,
                "The provided category IDs do not match the target group. " +
                "Supply exactly the same IDs as the group — no additions, omissions, or duplicates."
        );
    }
}