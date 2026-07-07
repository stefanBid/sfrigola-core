package com.sb.sfrigola_core.domains.tags.dto.view;

import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;

import java.io.Serializable;
import java.util.UUID;

public record TagDetailsAdminDto(
        UUID publicId,
        String slug,
        TagType type,
        TagScope scope,
        TagStatus status,
        String specificTranslationLabel
) implements Serializable {}
