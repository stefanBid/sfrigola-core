package com.sb.sfrigola_core.domains.categories.dto.input;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record UpdateCategoryDto(
        @ValidSlug
        String slug,

        @NotNull(message = CategoryValidationCodeConstants.IS_ACTIVE_REQUIRED)
        Boolean isActive,

        @NotNull(message = CategoryValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Valid UpsetCategoryTranslationDto specificTranslation
) implements Serializable {
}
