package com.sb.sfrigola_core.domains.ingredients.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientErrorCode;
import org.springframework.http.HttpStatus;

public class MissingIngredientLocalesException extends SCGeneralException {
    public MissingIngredientLocalesException() {
        super(
                HttpStatus.BAD_REQUEST,
                IngredientErrorCode.MISSING_INGREDIENT_LOCALES,
                "Translations must cover all active languages"
        );
    }
}
