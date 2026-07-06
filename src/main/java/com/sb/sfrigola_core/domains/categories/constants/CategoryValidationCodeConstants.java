package com.sb.sfrigola_core.domains.categories.constants;

public class CategoryValidationCodeConstants {
    private CategoryValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate CategoryValidationCodeConstants");
    }

    // --- TRANSLATIONS ---
    public static final String TRANSLATIONS_REQUIRED = "TRANSLATIONS_REQUIRED";
    public static final String TRANSLATIONS_MIN_ONE  = "TRANSLATIONS_MIN_ONE";

    // --- TRANSLATION LANG CODE ---
    public static final String LANG_CODE_REQUIRED    = "LANG_CODE_REQUIRED";

    // --- TRANSLATION NAME ---
    public static final String NAME_REQUIRED         = "NAME_REQUIRED";
    public static final String NAME_TOO_LONG         = "NAME_TOO_LONG";

    // --- TRANSLATION DESCRIPTION ---
    public static final String DESCRIPTION_TOO_LONG  = "DESCRIPTION_TOO_LONG";

    // --- IS ACTIVE ---
    public static final String IS_ACTIVE_REQUIRED    = "IS_ACTIVE_REQUIRED";

    // --- REORDER ---
    public static final String ORDERED_IDS_REQUIRED      = "ORDERED_IDS_REQUIRED";
    public static final String ORDERED_IDS_MIN_ONE        = "ORDERED_IDS_MIN_ONE";
    public static final String ORDERED_IDS_ELEMENT_NULL   = "ORDERED_IDS_ELEMENT_NULL";
}
