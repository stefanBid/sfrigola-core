package com.sb.sfrigola_core.domains.ratings.constants;

public class RatingValidationCodeConstants {

    private RatingValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate RatingValidationCodeConstants");
    }

    // --- RECIPE ---
    public static final String RECIPE_ID_REQUIRED       = "RECIPE_ID_REQUIRED";

    // --- SCORE ---
    public static final String SCORE_REQUIRED           = "SCORE_REQUIRED";
    public static final String SCORE_MUST_BE_AT_LEAST_ONE = "SCORE_MUST_BE_AT_LEAST_ONE";
    public static final String SCORE_MUST_BE_AT_MOST_FIVE  = "SCORE_MUST_BE_AT_MOST_FIVE";

    // --- COMMENT ---
    public static final String COMMENT_TOO_LONG         = "COMMENT_TOO_LONG";
}
