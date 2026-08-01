package com.sb.sfrigola_core.domains.recipes.dto.view;

import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RecipePreviewAdminDto(
        UUID publicId,
        UUID authorPublicId,
        UUID categoryPublicId,
        String imageUrl,
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
        String titlePreview,
        Map<String, String> translatedLanguages,
        BigDecimal avgRating,
        int ratingsCount,
        int favoritesCount
) implements Serializable {
}
