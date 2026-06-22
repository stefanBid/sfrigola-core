package com.sb.sfrigola_core.domains.users.constants;

public class UserValidationCodeConstants {
    private UserValidationCodeConstants() {
        throw new AssertionError("Cannot instantiate UserValidationCodeConstants");
    }

    // UPDATE PROFILE VALIDATION

    // --- USERNAME ---
    public static final String USERNAME_REQUIRED            = "USERNAME_REQUIRED";
    public static final String USERNAME_TOO_LONG            = "USERNAME_TOO_LONG";
    public static final String USERNAME_INVALID_FORMAT      = "USERNAME_INVALID_FORMAT";

    // --- EMAIL ---
    public static final String EMAIL_REQUIRED               = "EMAIL_REQUIRED";
    public static final String EMAIL_TOO_LONG               = "EMAIL_TOO_LONG";
    public static final String INVALID_EMAIL_FORMAT         = "INVALID_EMAIL_FORMAT";

    // --- PREFERRED LANGUAGE ---
    public static final String PREFERRED_LANG_REQUIRED      = "PREFERRED_LANG_REQUIRED";
    public static final String PREFERRED_LANG_INVALID_FORMAT = "PREFERRED_LANG_INVALID_FORMAT";

    // --- PROFILE ---
    public static final String FIRST_NAME_IS_REQUIRED       = "FIRST_NAME_IS_REQUIRED";
    public static final String FIRST_NAME_TOO_LONG          = "FIRST_NAME_TOO_LONG";
    public static final String LAST_NAME_IS_REQUIRED        = "LAST_NAME_IS_REQUIRED";
    public static final String LAST_NAME_TOO_LONG           = "LAST_NAME_TOO_LONG";
    public static final String AVATAR_URL_TOO_LONG          = "AVATAR_URL_TOO_LONG";
    public static final String BIO_TOO_LONG                 = "BIO_TOO_LONG";


    // --- ACTIVE STATUS ---
    public static final String IS_ACTIVE_IS_REQUIRED        = "IS_ACTIVE_IS_REQUIRED";
}