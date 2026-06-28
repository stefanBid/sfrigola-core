package com.sb.sfrigola_core.common.constant;

public class SCRequestParamValidationCodeConstants {

    private SCRequestParamValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate SCRequestParamValidationCodeConstants");
    }

    // --- PAGE ---
    public static final String PAGE_MUST_BE_AT_LEAST_ZERO       = "PAGE_MUST_BE_AT_LEAST_ZERO";

    // --- TAKE ---
    public static final String TAKE_MUST_BE_AT_LEAST_ONE        = "TAKE_MUST_BE_AT_LEAST_ONE";
    public static final String TAKE_MUST_BE_AT_MOST_HUNDRED     = "TAKE_MUST_BE_AT_MOST_HUNDRED";

    // --- SORT ---
    public static final String SORT_INVALID_VALUE            = "SORT_INVALID_VALUE";

    // --- LOCALE ---
    public static final String LOCALE_MUST_NOT_BE_BLANK          = "LOCALE_MUST_NOT_BE_BLANK";
}
