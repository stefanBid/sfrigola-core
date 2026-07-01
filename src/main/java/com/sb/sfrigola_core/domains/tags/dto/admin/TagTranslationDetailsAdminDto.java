package com.sb.sfrigola_core.domains.tags.dto.admin;

import java.io.Serializable;

public record TagTranslationDetailsAdminDto(
        String langCode,
        String langName,
        String label
) implements Serializable {
}
