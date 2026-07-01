package com.sb.sfrigola_core.domains.tags.dto;

import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagType;
import jakarta.validation.Valid;

import java.io.Serializable;
import java.util.List;

public record TagInputDto(
        String slug,
        TagType type,
        TagScope scope,

        List<@Valid TagTranslationInputDto> translations
) implements Serializable {
}
