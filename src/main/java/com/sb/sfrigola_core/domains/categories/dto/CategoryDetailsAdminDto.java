package com.sb.sfrigola_core.domains.categories.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record CategoryDetailsAdminDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        String namePreview,
        String descriptionPreview,
        int totalLocalization,
        int totalMissingLocalization,
        List<CategoryTranslationDto> translations,
        List<CategoryTranslationDto> missingTranslation
) implements Serializable {
}
