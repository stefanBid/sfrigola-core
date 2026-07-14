package com.sb.sfrigola_core.domains.recipes.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.recipes.enums.RecipeErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RecipeAuthorMismatchException extends SCGeneralException {
    public RecipeAuthorMismatchException(UUID publicId) {
        super(
                HttpStatus.FORBIDDEN,
                RecipeErrorCode.NOT_RECIPE_OWNER,
                "Recipe with ID: " + publicId + " does not belong to the authenticated contributor"
        );
    }
}
