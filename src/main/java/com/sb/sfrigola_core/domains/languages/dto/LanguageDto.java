package com.sb.sfrigola_core.domains.languages.dto;

import java.io.Serializable;

public record LanguageDto(
        String code,
        String name
) implements Serializable {
}