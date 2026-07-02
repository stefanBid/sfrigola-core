package com.sb.sfrigola_core.common.annotations.validations.slug;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SlugValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSlug {

    String message() default SlugConstants.SLUG_REQUIRED;
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
