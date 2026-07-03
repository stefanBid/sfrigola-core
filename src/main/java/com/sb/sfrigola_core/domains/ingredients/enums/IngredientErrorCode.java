package com.sb.sfrigola_core.domains.ingredients.enums;

import com.sb.sfrigola_core.common.interfaces.ISCErrorCode;

public enum IngredientErrorCode implements ISCErrorCode {

    INGREDIENT_NOT_FOUND,
    INGREDIENT_SLUG_ALREADY_EXISTS,
    INGREDIENT_LANGUAGE_NOT_ACTIVE,
    DUPLICATE_INGREDIENT_LOCALE;

    @Override
    public String code() {
        return name();
    }
}
