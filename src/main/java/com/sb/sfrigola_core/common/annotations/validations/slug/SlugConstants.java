package com.sb.sfrigola_core.common.annotations.validations.slug;

import java.util.regex.Pattern;

public class SlugConstants {

    private SlugConstants() {
        throw new AssertionError("Cannot instantiate SlugConstants");
    }

    // --- LENGTH CONSTRAINTS ---
    public static final int MAX_LENGTH = 100;

    // --- VALIDATION CODES ---
    public static final String SLUG_REQUIRED       = "SLUG_REQUIRED";
    public static final String SLUG_INVALID_FORMAT = "SLUG_INVALID_FORMAT";
    public static final String SLUG_TOO_LONG       = "SLUG_TOO_LONG";

    // --- REGEX ---
    public static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
}
