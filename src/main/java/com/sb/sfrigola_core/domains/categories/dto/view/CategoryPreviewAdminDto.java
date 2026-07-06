package com.sb.sfrigola_core.domains.categories.dto.view;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

public record CategoryPreviewAdminDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        CategoryTranslationAdminDto translationPreview,
        Map<String, String> translatedLanguages
) implements Serializable {
}
