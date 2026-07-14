package com.sb.sfrigola_core.domains.stats.enums;

import com.sb.sfrigola_core.common.interfaces.ISCSortField;
import lombok.Getter;

@Getter
public enum RecipeStatsSortField implements ISCSortField {
    FAVORITES_COUNT("favoritesCount");

    private final String entityFieldName;

    RecipeStatsSortField(String entityFieldName) { this.entityFieldName = entityFieldName; }

    public static RecipeStatsSortField fromString(String value) {
        return ISCSortField.fromString(RecipeStatsSortField.class, value);
    }
}
