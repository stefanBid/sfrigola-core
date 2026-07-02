package com.sb.sfrigola_core.domains.tags.dto.admin;

import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record TagDetailsAdminDto(
        UUID publicId,
        String slug,
        TagType type,
        TagScope scope,
        TagStatus status,
        String labelPreview,
        List<TagTranslationDetailsAdminDto> translations,
        List<TagTranslationDetailsAdminDto> missingTranslation
) implements Serializable {
}
