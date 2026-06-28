package com.sb.sfrigola_core.domains.categories.dto.admin;

import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record CategoryInputDto(
        @NotBlank(message = CategoryValidationCodeConstants.SLUG_REQUIRED)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = CategoryValidationCodeConstants.SLUG_INVALID_FORMAT)
        String slug,

        @NotNull(message = CategoryValidationCodeConstants.IS_ACTIVE_REQUIRED)
        Boolean isActive,

        @NotNull(message = CategoryValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = CategoryValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid CategoryTranslationInputDto> translations
) implements Serializable {
}