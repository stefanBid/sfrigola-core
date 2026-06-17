package com.sb.sfrigola_core.common.dto.external.response;

import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

public record SCErrorDataDto(
        String apiPath,
        HttpStatus status,
        Map<String, String> errorMessageMap,
        LocalDateTime errorTime
) implements Serializable {
}
