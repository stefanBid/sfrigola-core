package com.sb.sfrigola_core.domains.recipes.enums;

import com.sb.sfrigola_core.common.interfaces.ISCSortField;
import lombok.Getter;

@Getter
public enum RecipeSortField implements ISCSortField {
    TITLE("title"),
    DIFFICULTY("difficulty"),
    SEASON("season"),
    PREP_TIME_MIN("prepTimeMin"),
    COOK_TIME_MIN("cookTimeMin"),
    SERVINGS("servings"),
    IS_PUBLISHED("isPublished");

    private final String entityFieldName;

    RecipeSortField(String entityFieldName) { this.entityFieldName = entityFieldName; }

    public static RecipeSortField fromString(String value) {
        return ISCSortField.fromString(RecipeSortField.class, value);
    }
}
