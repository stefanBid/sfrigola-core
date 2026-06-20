package com.sb.sfrigola_core.domains.auth.dto;

import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;

import java.io.Serializable;

public record LoginResponseDto(
        SCUserExternalDto user,
        String role,
        String token
) implements Serializable {
}
