package com.sb.sfrigola_core.domains.categories.dto.admin;

import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record CategoryTranslationInputDto(
        @NotBlank(message = CategoryValidationCodeConstants.LANG_CODE_REQUIRED)
        String langCode,

        @Size(max = 100, message = CategoryValidationCodeConstants.NAME_TOO_LONG)
        String name,

        @Size(max = 500, message = CategoryValidationCodeConstants.DESCRIPTION_TOO_LONG)
        String description
) implements Serializable {
}