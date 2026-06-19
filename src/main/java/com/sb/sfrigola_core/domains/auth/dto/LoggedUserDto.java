package com.sb.sfrigola_core.domains.auth.dto;

import java.io.Serializable;

public record LoggedUserDto(
        SCUserMinimalInfoDto user,
        String role,
        String token
) implements Serializable {
}
