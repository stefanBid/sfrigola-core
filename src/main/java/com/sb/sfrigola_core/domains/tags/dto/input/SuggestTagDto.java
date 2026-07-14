package com.sb.sfrigola_core.domains.tags.dto.input;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.tags.constants.TagValidationCodeConstants;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record SuggestTagDto(
        @ValidSlug
        String slug,

        @NotNull(message = TagValidationCodeConstants.TYPE_REQUIRED)
        TagType type,

        @NotNull(message = TagValidationCodeConstants.SCOPE_REQUIRED)
        TagScope scope,

        @NotBlank(message = TagValidationCodeConstants.LABEL_REQUIRED)
        @Size(max = 100, message = TagValidationCodeConstants.LABEL_TOO_LONG)
        String translationByConsumerLang
) implements Serializable {}