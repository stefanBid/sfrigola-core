package com.sb.sfrigola_core.domains.recipes.dto.view;

import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record RecipeDetailsAdminDto(
        UUID publicId,
        UUID authorPublicId,
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
        boolean isPublished,
        String specificTranslationTitle,
        String specificTranslationDescription,
        String specificTranslationInstructions,
        List<RecipeIngredientDto> ingredients,
        List<TagDto> tags
) implements Serializable {
}
