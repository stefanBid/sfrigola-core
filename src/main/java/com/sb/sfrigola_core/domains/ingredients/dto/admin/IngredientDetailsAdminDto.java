package com.sb.sfrigola_core.domains.ingredients.dto.admin;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IngredientDetailsAdminDto(
        UUID publicId,
        String slug,
        String category,
        BigDecimal caloriesPer100g,
        String[] allergens,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree,
        String namePreview,
        List<IngredientTranslationDetailsAdminDto> translations,
        List<IngredientTranslationDetailsAdminDto> missingTranslation
) implements Serializable {
}
