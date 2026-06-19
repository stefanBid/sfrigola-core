package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.common.dto.external.response.SCErrorDataDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

public class SCErrorDataBuilderUtils {

    private SCErrorDataBuilderUtils(){
        throw new AssertionError("Cannot instantiate SCErrorDataBuilderUtils");
    }

    public static ResponseEntity<SCGeneralResponseDto<Void, Void>> build(HttpStatus status, Exception ex, WebRequest request) {
        return build(status, ex, ex.getMessage(), request);
    }

    public static ResponseEntity<SCGeneralResponseDto<Void, Void>> build(HttpStatus status, Exception ex, String message, WebRequest request) {
        return build(status, Map.of(ex.getClass().getSimpleName(), message), request);
    }

    public static ResponseEntity<SCGeneralResponseDto<Void, Void>> build(HttpStatus status, Map<String, String> messages, WebRequest request) {
        SCErrorDataDto errorData = createErrorData(status, messages, request);
        return ResponseEntity.status(status).body(SCGeneralResponseDto.error(errorData));
    }

    public static void handleError(HttpServletRequest request, HttpServletResponse response, ObjectMapper injectedObjectMapper,
                                   Map<String, String> errorMap, HttpStatus httpStatus) throws IOException {
        SCErrorDataDto errorData = new SCErrorDataDto(
                request.getRequestURI(),
                httpStatus,
                errorMap,
                LocalDateTime.now()
        );

        SCGeneralResponseDto<Void, Void> errorResponse = SCGeneralResponseDto.error(errorData);

        response.setStatus(httpStatus.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(injectedObjectMapper.writeValueAsString(errorResponse));
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