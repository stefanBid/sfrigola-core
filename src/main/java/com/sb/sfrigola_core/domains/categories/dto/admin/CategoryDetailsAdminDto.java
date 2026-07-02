package com.sb.sfrigola_core.domains.categories.dto.admin;

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
        List<CategoryDetailsTranslationAdminDto> translations,
        List<CategoryDetailsTranslationAdminDto> missingTranslation
) implements Serializable {
}
