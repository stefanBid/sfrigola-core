package com.sb.sfrigola_core.domains.categories.dto.view;

import java.io.Serializable;

public record CategoryTranslationAdminDto(
        String namePreview,
        String descriptionPreview
) implements Serializable {
}
