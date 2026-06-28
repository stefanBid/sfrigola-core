package com.sb.sfrigola_core.domains.categories.dto;

import java.io.Serializable;
import java.util.UUID;

public record CategoryDto(
        UUID publicId,
        String slug,
        UUID parentPublicId,
        short sortOrder,
        boolean isActive,
        String name,
        String description
) implements Serializable {}