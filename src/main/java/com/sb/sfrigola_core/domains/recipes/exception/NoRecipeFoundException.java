package com.sb.sfrigola_core.domains.recipes.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoRecipeFoundException extends SCGeneralException {
    public NoRecipeFoundException(UUID publicId) {
        super(
                HttpStatus.NOT_FOUND,
                RecipeErrorCode.RECIPE_NOT_FOUND,
                "No recipe found with ID: " + publicId
        );
    }
}
