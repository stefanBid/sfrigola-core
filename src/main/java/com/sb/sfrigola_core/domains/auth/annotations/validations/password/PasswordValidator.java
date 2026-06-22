package com.sb.sfrigola_core.domains.auth.annotations.validations.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (password == null || password.isBlank()) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_REQUIRED)
                    .addConstraintViolation();
            return false;
        }

        if (password.length() < PasswordConstants.MIN_LENGTH) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_TOO_SHORT)
                    .addConstraintViolation();
            valid = false;
        }

        if (password.length() > PasswordConstants.MAX_LENGTH) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_TOO_LONG)
                    .addConstraintViolation();
            valid = false;
        }

        if (!PasswordConstants.HAS_UPPERCASE.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_NO_UPPERCASE)
                    .addConstraintViolation();
            valid = false;
        }

        if (!PasswordConstants.HAS_LOWERCASE.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_NO_LOWERCASE)
                    .addConstraintViolation();
            valid = false;
        }

        if (!PasswordConstants.HAS_DIGIT.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_NO_DIGIT)
                    .addConstraintViolation();
            valid = false;
        }

        if (!PasswordConstants.HAS_SPECIAL_CHAR.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(PasswordConstants.PASSWORD_NO_SPECIAL_CHAR)
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}