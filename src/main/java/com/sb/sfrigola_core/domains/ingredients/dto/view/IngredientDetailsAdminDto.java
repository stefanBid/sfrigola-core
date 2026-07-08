package com.sb.sfrigola_core.domains.ingredients.dto.view;

import com.sb.sfrigola_core.domains.ingredients.enums.IngredientFoodGroup;
import com.sb.sfrigola_core.domains.tags.dto.view.TagDto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IngredientDetailsAdminDto(
        UUID publicId,
        String slug,
        IngredientFoodGroup foodGroup,
        BigDecimal caloriesPer100g,
        String[] allergens,
        boolean isVegetarian,
        boolean isVegan,
        boolean isGlutenFree,
        String specificTranslationName,
        List<TagDto> specificTranslationTagList
) implements Serializable {
}
