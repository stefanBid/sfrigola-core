package com.sb.sfrigola_core.domains.tags.dto;

import java.io.Serializable;

public record TagTranslationInputDto(
        String langCode,
        String label
) implements Serializable {
}
