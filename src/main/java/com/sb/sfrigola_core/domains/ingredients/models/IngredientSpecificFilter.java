package com.sb.sfrigola_core.domains.ingredients.models;

import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;

public record IngredientSpecificFilter(
        IngredientFoodGroup foodGroup,
        Boolean isVegetarian,
        Boolean isVegan,
        Boolean isGlutenFree,
        Double minCalories,
        Double maxCalories
) {
}
