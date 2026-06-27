package com.sb.sfrigola_core.domains.categories.dto.admin;

import java.io.Serializable;
import java.util.UUID;

public record CategoryPreviewAdminDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        CategoryPreviewTranslationAdminDto translationPreview,
        int totalLocalization,
        int totalMissingLocalization
) implements Serializable {
}
