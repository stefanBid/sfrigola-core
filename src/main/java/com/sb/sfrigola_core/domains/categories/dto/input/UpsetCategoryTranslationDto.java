package com.sb.sfrigola_core.domains.categories.dto.input;

import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record UpsetCategoryTranslationDto(
        @NotBlank(message = CategoryValidationCodeConstants.LANG_CODE_REQUIRED)
        String langCode,

        @NotBlank(message = CategoryValidationCodeConstants.NAME_REQUIRED)
        @Size(max = 100, message = CategoryValidationCodeConstants.NAME_TOO_LONG)
        String name,

        @Size(max = 500, message = CategoryValidationCodeConstants.DESCRIPTION_TOO_LONG)
        String description
) implements Serializable {
}
