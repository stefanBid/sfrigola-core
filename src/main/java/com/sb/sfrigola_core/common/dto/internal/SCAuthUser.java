package com.sb.sfrigola_core.common.dto.internal;

import java.time.Instant;

public record SCAuthUser(
        Long id,
        String username,
        String email,
        String ph,
        String preferredLang,
        String userRole, // Change with enum USERROLE when create auth feature
        Instant createdAt
) {
}
