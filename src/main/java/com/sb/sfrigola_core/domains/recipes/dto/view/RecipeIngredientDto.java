package com.sb.sfrigola_core.domains.recipes.dto.view;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record RecipeIngredientDto(
        UUID ingredientPublicId,
        String ingredientNamePreview,
        BigDecimal quantity,
        String unit,
        String preparationNote,
        short sortOrder
) implements Serializable {
}
