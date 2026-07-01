package com.sb.sfrigola_core.domains.categories.dto.admin;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record CategoryInputDto(

        @ValidSlug
        String slug,

        @NotNull(message = CategoryValidationCodeConstants.IS_ACTIVE_REQUIRED)
        Boolean isActive,

        @NotNull(message = CategoryValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = CategoryValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid CategoryTranslationInputDto> translations
) implements Serializable {
}