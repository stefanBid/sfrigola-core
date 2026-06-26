package com.sb.sfrigola_core.domains.categories.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.categories.enums.CategoryErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidCategoryLocaleException extends SCGeneralException {
    public InvalidCategoryLocaleException(String locale) {
        super(
                HttpStatus.BAD_REQUEST,
                CategoryErrorCode.INVALID_CATEGORY_LOCALE,
                "Locale '" + locale + "' is not active or does not exist"
        );
    }
}