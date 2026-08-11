package com.sb.sfrigola_core.domains.recipes.constants;

public class RecipeValidationCodeConstants {

    private RecipeValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate RecipeValidationCodeConstants");
    }

    // --- RECIPE IMAGE ---
    public static final String IMAGE_URL_TOO_LONG           = "IMAGE_URL_TOO_LONG";

    // --- TIME / SERVINGS ---
    public static final String PREP_TIME_MUST_BE_POSITIVE   = "PREP_TIME_MUST_BE_POSITIVE";
    public static final String COOK_TIME_MUST_BE_POSITIVE   = "COOK_TIME_MUST_BE_POSITIVE";
    public static final String SERVINGS_MUST_BE_POSITIVE    = "SERVINGS_MUST_BE_POSITIVE";

    // --- RECIPE INGREDIENTS ---
    public static final String INGREDIENT_ID_REQUIRED       = "INGREDIENT_ID_REQUIRED";
    public static final String QUANTITY_MUST_BE_POSITIVE    = "QUANTITY_MUST_BE_POSITIVE";
    public static final String UNIT_TOO_LONG                = "UNIT_TOO_LONG";
    public static final String PREPARATION_NOTE_TOO_LONG    = "PREPARATION_NOTE_TOO_LONG";

    // --- TRANSLATIONS ---
    public static final String TRANSLATIONS_REQUIRED        = "TRANSLATIONS_REQUIRED";
    public static final String TRANSLATIONS_MIN_ONE         = "TRANSLATIONS_MIN_ONE";

    // --- TRANSLATION LANG CODE ---
    public static final String LANG_CODE_REQUIRED           = "LANG_CODE_REQUIRED";

    // --- TRANSLATION TITLE ---
    public static final String TITLE_REQUIRED                = "TITLE_REQUIRED";
    public static final String TITLE_TOO_LONG                = "TITLE_TOO_LONG";

    // --- TRANSLATION INSTRUCTIONS ---
    public static final String INSTRUCTIONS_REQUIRED         = "INSTRUCTIONS_REQUIRED";
    public static final String INSTRUCTIONS_MIN_ONE          = "INSTRUCTIONS_MIN_ONE";
    public static final String INSTRUCTIONS_STEP_BLANK       = "INSTRUCTIONS_STEP_BLANK";
}
