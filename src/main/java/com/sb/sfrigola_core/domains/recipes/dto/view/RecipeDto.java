package com.sb.sfrigola_core.domains.recipes.dto.view;

import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;

import java.io.Serializable;
import java.util.UUID;

public record RecipeDto(
        UUID publicId,
        UUID categoryPublicId,
        DifficultyLevel difficulty,
        MealType mealType,
        SeasonType season,
        Integer prepTimeMin,
        Integer cookTimeMin,
        Short servings,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree,
        String title
) implements Serializable {
}
