package com.sb.sfrigola_core.domains.ingredients.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientErrorCode;
import org.springframework.http.HttpStatus;

public class IngredientLanguageNotActiveException extends SCGeneralException {
    public IngredientLanguageNotActiveException(String langCode) {
        super(
                HttpStatus.BAD_REQUEST,
                IngredientErrorCode.INGREDIENT_LANGUAGE_NOT_ACTIVE,
                "Language '" + langCode + "' is not active in the system"
        );
    }
}
