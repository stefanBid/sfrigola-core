package com.sb.sfrigola_core.domains.ingredients.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateIngredientLocaleException extends SCGeneralException {
    public DuplicateIngredientLocaleException(String locale) {
        super(
                HttpStatus.BAD_REQUEST,
                IngredientErrorCode.DUPLICATE_INGREDIENT_LOCALE,
                "Locale '" + locale + "' appears more than once in the translations list"
        );
    }
}
