package com.sb.sfrigola_core.domains.ingredients.dto.admin;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record IngredientPreviewAdminDto(
        UUID publicId,
        String slug,
        String category,
        BigDecimal caloriesPer100g,
        String[] allergens,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree,
        String namePreview,
        int totalLocalization,
        int totalMissingLocalization
) implements Serializable {
}
