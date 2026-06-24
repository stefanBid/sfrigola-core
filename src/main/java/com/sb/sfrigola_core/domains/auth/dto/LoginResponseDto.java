package com.sb.sfrigola_core.domains.auth.dto;

import com.sb.sfrigola_core.domains.users.dto.SCUserDto;

import java.io.Serializable;

public record LoginResponseDto(
        SCUserDto user,
        String role,
        String token
) implements Serializable {
}
