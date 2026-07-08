package com.sb.sfrigola_core.domains.recipes.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeErrorCode;
import org.springframework.http.HttpStatus;

public class DuplicateRecipeLocaleException extends SCGeneralException {
    public DuplicateRecipeLocaleException(String locale) {
        super(
                HttpStatus.BAD_REQUEST,
                RecipeErrorCode.DUPLICATE_RECIPE_LOCALE,
                "Locale '" + locale + "' appears more than once in the translations list"
        );
    }
}
