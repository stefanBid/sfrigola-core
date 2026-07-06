package com.sb.sfrigola_core.domains.ingredients.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientErrorCode;
import org.springframework.http.HttpStatus;

public class IngredientSlugAlreadyExistsException extends SCGeneralException {
    public IngredientSlugAlreadyExistsException(String slug) {
        super(
                HttpStatus.CONFLICT,
                IngredientErrorCode.INGREDIENT_SLUG_ALREADY_EXISTS,
                "An ingredient with slug '" + slug + "' already exists"
        );
    }
}
