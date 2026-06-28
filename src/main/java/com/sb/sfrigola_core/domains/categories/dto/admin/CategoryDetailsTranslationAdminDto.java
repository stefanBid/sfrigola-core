package com.sb.sfrigola_core.domains.categories.dto.admin;

import java.io.Serializable;

public record CategoryDetailsTranslationAdminDto(
        String langCode,
        String langName,
        String name,
        String description
) implements Serializable {
}
