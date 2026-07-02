package com.sb.sfrigola_core.domains.ingredients.models;

public record IngredientSpecificFilter(
        String category,
        Boolean isVegetarian,
        Boolean isVegan,
        Boolean isGlutenFree,
        Double minCalories,
        Double maxCalories
) {
}
