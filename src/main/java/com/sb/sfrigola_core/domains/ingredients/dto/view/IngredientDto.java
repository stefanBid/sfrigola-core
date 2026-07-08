package com.sb.sfrigola_core.domains.ingredients.dto.view;

import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record IngredientDto(
        UUID publicId,
        String slug,
        String name,
        IngredientFoodGroup foodGroup,
        BigDecimal caloriesPer100g,
        String[] allergens,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree
) implements Serializable {
}
