package com.sb.sfrigola_core.domains.auth.dto;

import java.io.Serializable;

public record LoginRequestDto(String username, String password) implements Serializable {
}
