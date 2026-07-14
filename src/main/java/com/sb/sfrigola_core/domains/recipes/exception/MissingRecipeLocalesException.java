package com.sb.sfrigola_core.domains.recipes.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeErrorCode;
import org.springframework.http.HttpStatus;

public class MissingRecipeLocalesException extends SCGeneralException {
    public MissingRecipeLocalesException() {
        super(
                HttpStatus.BAD_REQUEST,
                RecipeErrorCode.MISSING_RECIPE_LOCALES,
                "Translations must cover all active languages"
        );
    }
}
