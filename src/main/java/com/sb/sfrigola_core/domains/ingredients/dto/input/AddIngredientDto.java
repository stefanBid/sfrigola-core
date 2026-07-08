package com.sb.sfrigola_core.domains.ingredients.dto.input;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.ingredients.constants.IngredientValidationCodeConstants;
import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddIngredientDto(
        @ValidSlug(maxLength = 150)
        String slug,

        IngredientFoodGroup foodGroup,

        @DecimalMin(value = "0.0", message = IngredientValidationCodeConstants.CALORIES_MUST_BE_POSITIVE)
        BigDecimal caloriesPer100g,

        String[] allergens,

        boolean isVegetarian,

        boolean isVegan,

        boolean isGlutenFree,

        List<UUID> ingredientTagsIds,

        @NotNull(message = IngredientValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = IngredientValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid IngredientTranslationInputDto> translations
) implements Serializable {
}
