package com.sb.sfrigola_core.domains.categories.dto.view;

import java.io.Serializable;
import java.util.UUID;

public record CategoryDetailsAdminDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        CategoryTranslationAdminDto specificTranslation
) implements Serializable {
}
