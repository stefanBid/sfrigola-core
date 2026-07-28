package com.sb.sfrigola_core.common.constant;

public class SCGeneralConstants {

    private SCGeneralConstants() {
        throw new AssertionError("Cannot instantiate SCGeneralConstants");
    }

    public static final String ALLOWED_ORIGINS = "ALLOWED_ORIGINS";
    public static final String SYSTEM_USERNAME = "system";

    // JWT TOKEN CONSTANTS
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_HEADER_PREFIX = "Bearer ";
    public static final String JWT_SECRET_KEY = "JWT_SECRET_KEY";
    public static final String JWT_EXPIRATION_MS = "JWT_EXPIRATION_MS";
}
