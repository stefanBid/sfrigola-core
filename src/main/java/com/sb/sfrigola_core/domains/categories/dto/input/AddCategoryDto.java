package com.sb.sfrigola_core.domains.categories.dto.input;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record AddCategoryDto(
        @ValidSlug
        String slug,

        @NotNull(message = CategoryValidationCodeConstants.IS_ACTIVE_REQUIRED)
        Boolean isActive,

        // First level Check (limit case only one lang)
        @NotNull(message = CategoryValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = CategoryValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid UpsetCategoryTranslationDto> translations
) implements Serializable {
}
