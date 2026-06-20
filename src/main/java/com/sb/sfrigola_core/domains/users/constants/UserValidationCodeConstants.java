package com.sb.sfrigola_core.domains.users.constants;

public class UserValidationCodeConstants {
    private UserValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate UserValidationCodeConstants");
    }

    // --- USERNAME ---
    public static final String USERNAME_REQUIRED            = "USERNAME_REQUIRED";
    public static final String USERNAME_TOO_LONG            = "USERNAME_TOO_LONG";
    public static final String USERNAME_INVALID_FORMAT      = "USERNAME_INVALID_FORMAT";

    // --- EMAIL ---
    public static final String EMAIL_REQUIRED               = "EMAIL_REQUIRED";
    public static final String EMAIL_TOO_LONG               = "EMAIL_TOO_LONG";
    public static final String INVALID_EMAIL_FORMAT         = "INVALID_EMAIL_FORMAT";

    // --- PASSWORD ---
    public static final String PASSWORD_REQUIRED            = "PASSWORD_REQUIRED";
    public static final String PASSWORD_TOO_SHORT           = "PASSWORD_TOO_SHORT";
    public static final String PASSWORD_TOO_LONG            = "PASSWORD_TOO_LONG";

    // --- PREFERRED LANGUAGE ---
    public static final String PREFERRED_LANG_REQUIRED      = "PREFERRED_LANG_REQUIRED";
    public static final String PREFERRED_LANG_INVALID_FORMAT = "PREFERRED_LANG_INVALID_FORMAT";

    // --- PROFILE ---
    public static final String FIRST_NAME_TOO_LONG          = "FIRST_NAME_TOO_LONG";
    public static final String LAST_NAME_TOO_LONG           = "LAST_NAME_TOO_LONG";
    public static final String AVATAR_URL_TOO_LONG          = "AVATAR_URL_TOO_LONG";
    public static final String BIO_TOO_LONG                 = "BIO_TOO_LONG";
}