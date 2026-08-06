package com.sb.sfrigola_core.common.annotations.validations.image;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidImage {
    String message() default ImageConstants.IMAGE_INVALID_FORMAT;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    long maxSizeMb() default 5; // Maximum size in megabytes
    long minSizeMb() default 0; // Minimum size in megabytes
    long maxWidthPx() default 0; // Maximum width in pixels (0 means no limit)
    long maxHeightPx() default 0; // Maximum height in pixels (0 means no limit)
    long minWidthPx() default 0; // Minimum width in pixels (0 means no limit)
    long minHeightPx() default 0; // Minimum height in pixels (0 means no limit)
    double minAspectRatio() default 0; // Minimum aspect ratio (width/height) (0 means no limit)
    double maxAspectRatio() default 0; // Maximum aspect ratio (width/height) (0 means no limit)
    String[] allowedFormats() default {
            ImageConstants.EXT_JPG, ImageConstants.EXT_JPEG, ImageConstants.EXT_PNG,
            ImageConstants.EXT_GIF, ImageConstants.EXT_BMP, ImageConstants.EXT_WEBP
    };
}

