package com.sb.sfrigola_core.common.constant;

public class SCGeneralConstants {

    private SCGeneralConstants() {
        throw new AssertionError("Cannot instantiate SCGeneralConstants");
    }

    public static final String ALLOWED_ORIGINS = "ALLOWED_ORIGINS";
    public static final String JWT_SECRET_KEY = "JWT_SECRET_KEY";
    public static final String JWT_SECRET_KEY_DEFAULT = "mySecretKeyForJWTGenerationWhichShouldBeLongEnoughToBeSecure";
    public static final String JWT_EXPIRATION_MS = "JWT_EXPIRATION_MS";
    public static final String JWT_HEADER = "JWT_HEADER";
    public static final String JWT_HEADER_DEFAULT = "Authorization";
    public static final String SYSTEM_USERNAME = "system";
}
