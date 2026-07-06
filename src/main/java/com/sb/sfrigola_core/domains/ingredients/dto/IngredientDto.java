package com.sb.sfrigola_core.domains.ingredients.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record IngredientDto(
        UUID publicId,
        String slug,
        String name,
        String category,
        BigDecimal caloriesPer100g,
        String[] allergens,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree
) implements Serializable {
}
