package com.sb.sfrigola_core.domains.tags.dto.input;

import com.sb.sfrigola_core.common.annotations.validations.slug.ValidSlug;
import com.sb.sfrigola_core.domains.tags.constants.TagValidationCodeConstants;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record UpdateTagDto(
        @ValidSlug
        String slug,

        @NotNull(message = TagValidationCodeConstants.TYPE_REQUIRED)
        TagType type,

        @NotNull(message = TagValidationCodeConstants.SCOPE_REQUIRED)
        TagScope scope,

        @NotNull(message = TagValidationCodeConstants.TRANSLATIONS_REQUIRED)
        @Valid TagTranslationInputDto specificTranslation
) implements Serializable {}