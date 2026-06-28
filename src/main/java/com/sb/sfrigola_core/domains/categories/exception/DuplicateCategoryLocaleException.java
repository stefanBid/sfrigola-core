package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateCategoryLocaleException extends SCGeneralException {
    public DuplicateCategoryLocaleException(String locale) {
        super(
                HttpStatus.BAD_REQUEST,
                CategoryErrorCode.DUPLICATE_CATEGORY_LOCALE,
                "Locale '" + locale + "' appears more than once in the translations list"
        );
    }
}