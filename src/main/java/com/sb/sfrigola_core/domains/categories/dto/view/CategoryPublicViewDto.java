package com.sb.sfrigola_core.domains.categories.dto.view;

import java.io.Serializable;
import java.util.UUID;

public record CategoryPublicViewDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        String name,
        String description
) implements Serializable {}