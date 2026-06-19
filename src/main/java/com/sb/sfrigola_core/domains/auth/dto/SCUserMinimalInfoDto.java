package com.sb.sfrigola_core.domains.auth.dto;

import java.io.Serializable;
import java.util.UUID;

public record SCUserMinimalInfoDto(
        UUID publicId,
        String username,
        String email,
        String preferredLang,
        boolean isActive,
        String firstName,
        String lastName
) implements Serializable {
}
