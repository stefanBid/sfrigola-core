package com.sb.sfrigola_core.domains.recipes.dto.input;

import com.sb.sfrigola_core.domains.recipes.constants.RecipeValidationCodeConstants;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientInputDto(
        @NotNull(message = RecipeValidationCodeConstants.INGREDIENT_ID_REQUIRED)
        UUID ingredientPublicId,

        @DecimalMin(value = "0.0", message = RecipeValidationCodeConstants.QUANTITY_MUST_BE_POSITIVE)
        BigDecimal quantity,

        @Size(max = 50, message = RecipeValidationCodeConstants.UNIT_TOO_LONG)
        String unit,

        @Size(max = 200, message = RecipeValidationCodeConstants.PREPARATION_NOTE_TOO_LONG)
        String preparationNote,

        short sortOrder
) implements Serializable {
}
