package com.sb.sfrigola_core.domains.ingredients.enums;

import com.sb.sfrigola_core.common.interfaces.ISCSortField;
import lombok.Getter;

@Getter
public enum IngredientSortField implements ISCSortField {
    NAME("name"),
    SLUG("slug"),
    FOOD_GROUP("foodGroup"),
    CALORIES_PER_100G("caloriesPer100g"),
    IS_VEGETARIAN("isVegetarian"),
    IS_VEGAN("isVegan"),
    IS_GLUTEN_FREE("isGlutenFree");

    private final String entityFieldName;

    IngredientSortField(String entityFieldName) { this.entityFieldName = entityFieldName; }

    public static IngredientSortField fromString(String value) {
        return ISCSortField.fromString(IngredientSortField.class, value);
    }
}
