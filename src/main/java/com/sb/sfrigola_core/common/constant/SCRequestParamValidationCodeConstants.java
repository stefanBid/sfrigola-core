package com.sb.sfrigola_core.common.constant;

public class SCRequestParamValidationCodeConstants {

    private SCRequestParamValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate SCRequestParamValidationCodeConstants");
    }

    // --- PAGE ---
    public static final String PAGE_MUST_BE_GTE_ZERO         = "PAGE_MUST_BE_GTE_ZERO";

    // --- TAKE ---
    public static final String TAKE_MUST_BE_GTE_ONE          = "TAKE_MUST_BE_GTE_ONE";
    public static final String TAKE_MUST_BE_LTE_HUNDRED      = "TAKE_MUST_BE_LTE_HUNDRED";

    // --- SORT ---
    public static final String SORT_INVALID_VALUE            = "SORT_INVALID_VALUE";
}
