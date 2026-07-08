package com.sb.sfrigola_core.domains.recipes.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum RecipeErrorCode implements ISCErrorCode {

    RECIPE_NOT_FOUND,
    DUPLICATE_RECIPE_LOCALE,
    MISSING_RECIPE_LOCALES,
    NOT_RECIPE_OWNER;

    @Override
    public String code() {
        return name();
    }
}
