package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.common.dto.external.response.SCErrorDataDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Map;

public class SCErrorDataBuilderUtils {

    private SCErrorDataBuilderUtils(){
        throw new AssertionError("Cannot instantiate SCErrorDataBuilderUtils");
    }

    public static  ResponseEntity<SCGeneralResponseDto<Void, Void>> build(HttpStatus status, Exception ex, WebRequest request) {
        return build(status, ex, ex.getMessage(), request);
    }

    public static ResponseEntity<SCGeneralResponseDto<Void, Void>> build(HttpStatus status, Exception ex, String message, WebRequest request) {
        return build(status, Map.of(ex.getClass().getSimpleName(), message), request);
    }

    public static ResponseEntity<SCGeneralResponseDto<Void, Void>> build(HttpStatus status, Map<String, String> messages, WebRequest request) {
        SCErrorDataDto errorData = createErrorData(status, messages, request);
        return ResponseEntity.status(status).body(SCGeneralResponseDto.error(errorData));
    }


    // ========== SHARED ERROR DATA CREATION ==========

    private static SCErrorDataDto createErrorData(HttpStatus status, Map<String, String> messages, WebRequest request) {
        return new SCErrorDataDto(
                request.getDescription(false),
                status,
                messages,
                LocalDateTime.now()
        );
    }

}
