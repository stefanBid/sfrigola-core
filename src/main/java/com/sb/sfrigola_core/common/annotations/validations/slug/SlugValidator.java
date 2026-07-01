package com.sb.sfrigola_core.common.annotations.validations.slug;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SlugValidator implements ConstraintValidator<ValidSlug, String> {

    @Override
    public boolean isValid(String slug, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (slug == null || slug.isBlank()) {
            context.buildConstraintViolationWithTemplate(SlugConstants.SLUG_REQUIRED)
                    .addConstraintViolation();
            return false;
        }

        if (slug.length() > SlugConstants.MAX_LENGTH) {
            context.buildConstraintViolationWithTemplate(SlugConstants.SLUG_TOO_LONG)
                    .addConstraintViolation();
            return false;
        }

        if (!SlugConstants.SLUG_PATTERN.matcher(slug).matches()) {
            context.buildConstraintViolationWithTemplate(SlugConstants.SLUG_INVALID_FORMAT)
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
