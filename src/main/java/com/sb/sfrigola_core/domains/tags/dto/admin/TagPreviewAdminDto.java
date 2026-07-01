package com.sb.sfrigola_core.domains.tags.dto.admin;

import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;

import java.io.Serializable;
import java.util.UUID;

public record TagPreviewAdminDto(
        UUID publicId,
        String slug,
        TagType type,
        TagScope scope,
        TagStatus status,
        String labelPreview,
        int totalLocalization,
        int totalMissingLocalization
) implements Serializable {
}
