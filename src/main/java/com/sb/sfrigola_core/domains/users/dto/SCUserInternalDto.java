package com.sb.sfrigola_core.domains.users.dto;

import com.sb.sfrigola_core.domains.users.enums.SCUserRole;

import java.io.Serializable;
import java.util.UUID;

public record SCUserInternalDto(
        UUID publicId,
        SCUserRole role,
        String username,
        String email,
        String pHash,
        String preferredLang,
        boolean isActive,
        String firstName,
        String lastName
) implements Serializable {


}
