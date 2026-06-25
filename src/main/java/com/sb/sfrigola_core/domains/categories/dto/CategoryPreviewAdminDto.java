package com.sb.sfrigola_core.domains.categories.dto;

import java.io.Serializable;
import java.util.UUID;

public record CategoryPreviewAdminDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        String namePreview,
        String descriptionPreview,
        int totalLocalization,
        int totalMissingLocalization
) implements Serializable {
}
