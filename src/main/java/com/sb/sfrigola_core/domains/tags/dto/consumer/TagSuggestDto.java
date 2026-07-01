package com.sb.sfrigola_core.domains.tags.dto.consumer;

import com.sb.sfrigola_core.domains.tags.constants.TagValidationCodeConstants;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record TagSuggestDto(
        @NotBlank(message = TagValidationCodeConstants.SLUG_REQUIRED)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = TagValidationCodeConstants.SLUG_INVALID_FORMAT)
        @Size(max = 100, message = TagValidationCodeConstants.SLUG_TOO_LONG)
        String slug,

        @NotNull(message = TagValidationCodeConstants.TYPE_REQUIRED)
        TagType type,

        @NotNull(message = TagValidationCodeConstants.SCOPE_REQUIRED)
        TagScope scope,

        @NotBlank(message = TagValidationCodeConstants.LABEL_REQUIRED)
        @Size(max = 100, message = TagValidationCodeConstants.LABEL_TOO_LONG)
        String translationByConsumerLang
) implements Serializable {
}