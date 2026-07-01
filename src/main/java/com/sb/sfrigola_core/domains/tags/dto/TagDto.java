package com.sb.sfrigola_core.domains.tags.dto;

import java.io.Serializable;
import java.util.UUID;

public record TagDto(
        UUID publicId,
        String slug,
        String label
) implements Serializable {
}
