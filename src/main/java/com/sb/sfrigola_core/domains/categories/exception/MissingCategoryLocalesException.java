package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

public class MissingCategoryLocalesException extends SCGeneralException {
    public MissingCategoryLocalesException() {
        super(
                HttpStatus.BAD_REQUEST,
                CategoryErrorCode.MISSING_CATEGORY_LOCALES,
                "Translations must cover all active languages"
        );
    }
}