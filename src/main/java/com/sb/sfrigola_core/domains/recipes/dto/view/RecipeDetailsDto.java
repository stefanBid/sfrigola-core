package com.sb.sfrigola_core.domains.recipes.dto.view;

import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecipeDetailsDto(
        UUID publicId,
        UUID authorPublicId,
        UUID categoryPublicId,
        DifficultyLevel difficulty,
        String imageUrl,
        MealType mealType,
        SeasonType season,
        Integer prepTimeMin,
        Integer cookTimeMin,
        Short servings,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree,
        String specificTranslationTitle,
        String specificTranslationDescription,
        String specificTranslationInstructions,
        List<RecipeIngredientDto> ingredients,
        List<TagDto> tags,
        boolean isFavourite,
        BigDecimal avgRating,
        int ratingsCount,
        int favoritesCount
) implements Serializable {
}
