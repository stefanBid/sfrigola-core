package com.sb.sfrigola_core.domains.ingredients.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoIngredientFoundException extends SCGeneralException {
    public NoIngredientFoundException(UUID publicId) {
        super(
                HttpStatus.NOT_FOUND,
                IngredientErrorCode.INGREDIENT_NOT_FOUND,
                "No ingredient found with ID: " + publicId
        );
    }
}
