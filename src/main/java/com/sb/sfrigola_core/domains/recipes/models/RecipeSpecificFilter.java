package com.sb.sfrigola_core.domains.recipes.models;

import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;

public record RecipeSpecificFilter(
        DifficultyLevel difficulty,
        MealType mealType,
        SeasonType season,
        Boolean isVegetarian,
        Boolean isVegan,
        Boolean isGlutenFree,
        Boolean isPublished
) {
}
