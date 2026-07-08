package com.sb.sfrigola_core.domains.ingredients.constants;

public class IngredientValidationCodeConstants {

    private IngredientValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate IngredientValidationCodeConstants");
    }

    // --- CALORIES ---
    public static final String CALORIES_MUST_BE_POSITIVE   = "CALORIES_MUST_BE_POSITIVE";

    // --- TRANSLATIONS ---
    public static final String TRANSLATIONS_REQUIRED       = "TRANSLATIONS_REQUIRED";
    public static final String TRANSLATIONS_MIN_ONE        = "TRANSLATIONS_MIN_ONE";

    // --- TRANSLATION LANG CODE ---
    public static final String LANG_CODE_REQUIRED          = "LANG_CODE_REQUIRED";

    // --- TRANSLATION NAME ---
    public static final String NAME_TOO_LONG                = "NAME_TOO_LONG";
}
