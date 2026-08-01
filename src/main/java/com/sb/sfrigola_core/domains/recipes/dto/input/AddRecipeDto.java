package com.sb.sfrigola_core.domains.recipes.dto.input;

import com.sb.sfrigola_core.domains.recipes.constants.RecipeValidationCodeConstants;
import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record AddRecipeDto(
        UUID categoryPublicId,

        @NotNull
        DifficultyLevel difficulty,

        MealType mealType,

        @Size(max = 1000, message = RecipeValidationCodeConstants.IMAGE_URL_TOO_LONG)
        String imageUrl,

        @NotNull
        SeasonType season,

        @Min(value = 0, message = RecipeValidationCodeConstants.PREP_TIME_MUST_BE_POSITIVE)
        Integer prepTimeMin,

        @Min(value = 0, message = RecipeValidationCodeConstants.COOK_TIME_MUST_BE_POSITIVE)
        Integer cookTimeMin,

        @Min(value = 1, message = RecipeValidationCodeConstants.SERVINGS_MUST_BE_POSITIVE)
        Short servings,

        boolean isVegetarian,

        boolean isVegan,

        boolean isGlutenFree,

        List<UUID> recipeTagsIds,

        List<@Valid RecipeIngredientInputDto> ingredients,

        @NotNull(message = RecipeValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = RecipeValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid RecipeTranslationInputDto> translations
) implements Serializable {
}
