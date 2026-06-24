package com.sb.sfrigola_core.domains.users.dto;

import java.io.Serializable;
import java.util.UUID;

public record SCUserDto(
        UUID publicId,
        String username,
        String email,
        String preferredLang,
        boolean isActive,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio
) implements Serializable {
    public static SCUserDto minimalInfo(UUID publicId, String username, String email, String preferredLang, boolean isActive, String firstName, String lastName) {
        return new SCUserDto(publicId, username, email, preferredLang, isActive, firstName, lastName, null, null);
    }


}
