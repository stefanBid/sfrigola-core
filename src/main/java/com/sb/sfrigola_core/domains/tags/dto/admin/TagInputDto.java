package com.sb.sfrigola_core.domains.tags.dto.admin;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.tags.constants.TagValidationCodeConstants;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record TagInputDto(
        @ValidSlug
        String slug,

        @NotNull(message = TagValidationCodeConstants.TYPE_REQUIRED)
        TagType type,

        @NotNull(message = TagValidationCodeConstants.SCOPE_REQUIRED)
        TagScope scope,

        @NotNull(message = TagValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Size(min = 1, message = TagValidationCodeConstants.TRANSLATIONS_MIN_ONE)
        List<@Valid TagTranslationInputDto> translations
) implements Serializable {
}
