package com.sb.sfrigola_core.domains.ingredients.dto.admin;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.ingredients.constants.IngredientValidationCodeConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IngredientInputDto(
        @ValidSlug(maxLength = 150)
        String slug,

        @Size(max = 100, message = IngredientValidationCodeConstants.CATEGORY_TOO_LONG)
        String category,

        @DecimalMin(value = "0.0", message = IngredientValidationCodeConstants.CALORIES_MUST_BE_POSITIVE)
        BigDecimal caloriesPer100g,

        String[] allergens,

        boolean isVegetarian,

        boolean isVegan,

        boolean isGlutenFree,

        @NotNull(message = IngredientValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = IngredientValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid IngredientTranslationInputDto> translations,

        List<UUID> ingredientTagsIds
) implements Serializable {
}
