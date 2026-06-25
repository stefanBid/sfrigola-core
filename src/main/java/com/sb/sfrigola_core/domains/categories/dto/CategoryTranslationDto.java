package com.sb.sfrigola_core.domains.categories.dto;

import java.io.Serializable;

public record CategoryTranslationDto(
        String langCode,
        String langName,
        String name,
        String description
) implements Serializable {
}
