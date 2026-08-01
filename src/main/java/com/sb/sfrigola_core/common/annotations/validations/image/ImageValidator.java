package com.sb.sfrigola_core.common.annotations.validations.image;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ImageValidator implements ConstraintValidator<ValidImage, String> {

    private Set<String> allowedFormats;

    // Guards against dev typos in @ValidImage(allowedFormats = ...): a stray leading dot (".gif"),
    // stray whitespace, or mixed case would otherwise never match declaredExtension (always bare, lowercase).
    private Set<String> purifyAllowedFormats(String[] rawFormats) {
        return Arrays.stream(rawFormats)
                .map(af -> af.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.+", ""))
                .filter(af -> !af.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.allowedFormats = purifyAllowedFormats(constraintAnnotation.allowedFormats());
    }

    @Override
    public boolean isValid(String imageValue, ConstraintValidatorContext context) {
        if (imageValue == null) return true;
        context.disableDefaultConstraintViolation();
        Matcher dataUriMatcher = ImageConstants.DATA_URI_IMAGE_PATTERN.matcher(imageValue);
        Matcher filePathMatcher = ImageConstants.FILE_PATH_PATTERN.matcher(imageValue);
        boolean isDataUri = dataUriMatcher.matches();
        boolean isFilePath = filePathMatcher.matches();

        // Check format
        if (!isDataUri && !isFilePath) {
            context.buildConstraintViolationWithTemplate(ImageConstants.IMAGE_INVALID_FORMAT)
                    .addConstraintViolation();
            return false;
        }

        // After passing the format check, split the extension-check by branch: Data URI carries
        // its subtype in the regex group, a plain path/URL carries it after the last dot.
        String declaredExtension = isDataUri
                ? dataUriMatcher.group(1).toLowerCase()
                : filePathMatcher.group().substring(filePathMatcher.group().lastIndexOf('.') + 1).toLowerCase();

        if (!allowedFormats.contains(declaredExtension)) {
            context.buildConstraintViolationWithTemplate(ImageConstants.IMAGE_INVALID_FORMAT)
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
