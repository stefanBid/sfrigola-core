package com.sb.sfrigola_core.domains.categories.dto.admin;

import java.io.Serializable;

public record CategoryPreviewTranslationAdminDto(
        String namePreview,
        String descriptionPreview
) implements Serializable {
}
