package com.sb.sfrigola_core.common.annotations.validations.image;

import java.util.List;
import java.util.regex.Pattern;

public class ImageConstants {
    private ImageConstants() { throw new AssertionError("Cannot instantiate ImageConstants"); }

    // --- PRESENCE / READABILITY ---
    public static final String IMAGE_REQUIRED        = "IMAGE_REQUIRED";
    public static final String IMAGE_UNREADABLE      = "IMAGE_UNREADABLE";
    public static final String IMAGE_INVALID_FORMAT  = "IMAGE_INVALID_FORMAT";

    // --- FILE SIZE (maxSizeMb / minSizeMb) ---
    public static final String IMAGE_SIZE_TOO_LARGE  = "IMAGE_SIZE_TOO_LARGE";
    public static final String IMAGE_SIZE_TOO_SMALL  = "IMAGE_SIZE_TOO_SMALL";

    // --- WIDTH (maxWidthPx / minWidthPx) ---
    public static final String IMAGE_WIDTH_TOO_LARGE = "IMAGE_WIDTH_TOO_LARGE";
    public static final String IMAGE_WIDTH_TOO_SMALL = "IMAGE_WIDTH_TOO_SMALL";

    // --- HEIGHT (maxHeightPx / minHeightPx) ---
    public static final String IMAGE_HEIGHT_TOO_LARGE = "IMAGE_HEIGHT_TOO_LARGE";
    public static final String IMAGE_HEIGHT_TOO_SMALL = "IMAGE_HEIGHT_TOO_SMALL";

    // --- REGEX (structural shape only — extension allow-list checked separately via SCImageValidatorUtils) ---

    // data:image/<subtype>;base64,<payload> — subtype captured in group(1), generic token, not a fixed extension list
    public static final Pattern DATA_URI_IMAGE_PATTERN =
            Pattern.compile("^data:image/([a-zA-Z0-9.+-]+);base64,[A-Za-z0-9+/]+={0,2}$");

    // plain filename or path ending in a dot-extension — matches "photo.jpg", "/uploads/photo.jpg", "C:\\Users\\x\\photo.png"
    public static final Pattern FILE_PATH_PATTERN =
            Pattern.compile("^(?:[\\w.\\-]+[/\\\\])*[\\w\\-. ]+\\.[a-zA-Z0-9]+$");


    // --- ALLOWED FORMATS (default group used by ValidImage.allowedFormats() when not overridden) ---
    public static final String EXT_JPG  = "jpg";
    public static final String EXT_JPEG = "jpeg";
    public static final String EXT_PNG  = "png";
    public static final String EXT_GIF  = "gif";
    public static final String EXT_BMP  = "bmp";
    public static final String EXT_WEBP = "webp";

    public static final List<String> ALLOWED_IMAGE_FORMATS =
            List.of(EXT_JPG, EXT_JPEG, EXT_PNG, EXT_GIF, EXT_BMP, EXT_WEBP);
}
