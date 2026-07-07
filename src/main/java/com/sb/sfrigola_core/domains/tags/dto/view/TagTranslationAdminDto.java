package com.sb.sfrigola_core.domains.tags.dto.view;

import java.io.Serializable;

public record TagTranslationAdminDto(
        String langCode,
        String langName,
        String label
) implements Serializable {}