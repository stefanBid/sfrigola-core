package com.sb.sfrigola_core.domains.ingredients.dto.view;

import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record IngredientPreviewAdminDto(
        UUID publicId,
        String slug,
        IngredientFoodGroup foodGroup,
        BigDecimal caloriesPer100g,
        String[] allergens,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree,
        String namePreview,
        Map<String, String> translatedLanguages
) implements Serializable {
}
