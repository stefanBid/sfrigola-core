package com.sb.sfrigola_core.domains.auth.annotations.validations.password;

import java.util.regex.Pattern;

public class PasswordConstants {
    private PasswordConstants() {
        throw new AssertionError("Cannot instantiate PasswordConstants");
    }

    // --- LENGTH CONSTRAINTS ---
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    // --- VALIDATION CODES ---
    public static final String PASSWORD_REQUIRED        = "PASSWORD_REQUIRED";
    public static final String PASSWORD_TOO_SHORT       = "PASSWORD_TOO_SHORT";
    public static final String PASSWORD_TOO_LONG        = "PASSWORD_TOO_LONG";
    public static final String PASSWORD_NO_UPPERCASE    = "PASSWORD_NO_UPPERCASE";
    public static final String PASSWORD_NO_LOWERCASE    = "PASSWORD_NO_LOWERCASE";
    public static final String PASSWORD_NO_DIGIT        = "PASSWORD_NO_DIGIT";
    public static final String PASSWORD_NO_SPECIAL_CHAR = "PASSWORD_NO_SPECIAL_CHAR";
    public static final String PASSWORD_WEAK            = "PASSWORD_WEAK";

    // --- REGEX ---
    public static final Pattern HAS_UPPERCASE    = Pattern.compile(".*[A-Z].*");
    public static final Pattern HAS_LOWERCASE    = Pattern.compile(".*[a-z].*");
    public static final Pattern HAS_DIGIT        = Pattern.compile(".*[0-9].*");
    public static final Pattern HAS_SPECIAL_CHAR = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
}