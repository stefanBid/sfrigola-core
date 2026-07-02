package com.sb.sfrigola_core.domains.tags.dto.admin;

import com.sb.sfrigola_core.domains.tags.constants.TagValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record TagTranslationInputDto(
        @NotBlank(message = TagValidationCodeConstants.LANG_CODE_REQUIRED)
        String langCode,

        @NotBlank(message = TagValidationCodeConstants.LABEL_REQUIRED)
        @Size(max = 100, message = TagValidationCodeConstants.LABEL_TOO_LONG)
        String label
) implements Serializable {
}