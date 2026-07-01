package com.sb.sfrigola_core.domains.tags.constants;

public class TagValidationCodeConstants {

    private TagValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate TagValidationCodeConstants");
    }

    // --- TYPE / SCOPE ---
    public static final String TYPE_REQUIRED         = "TYPE_REQUIRED";
    public static final String SCOPE_REQUIRED        = "SCOPE_REQUIRED";

    // --- LABEL ---
    public static final String LABEL_REQUIRED        = "LABEL_REQUIRED";
    public static final String LABEL_TOO_LONG        = "LABEL_TOO_LONG";

    // --- TRANSLATIONS ---
    public static final String TRANSLATIONS_REQUIRED = "TRANSLATIONS_REQUIRED";
    public static final String TRANSLATIONS_MIN_ONE   = "TRANSLATIONS_MIN_ONE";

    // --- TRANSLATION LANG CODE ---
    public static final String LANG_CODE_REQUIRED    = "LANG_CODE_REQUIRED";
}