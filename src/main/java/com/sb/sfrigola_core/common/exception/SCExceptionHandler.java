package com.sb.sfrigola_core.common.exception;

import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.enums.GeneralErrorCode;
import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.common.util.SCErrorDataBuilderUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

    @RestControllerAdvice
public class SCExceptionHandler {

    // 404 — Entity not found (e.g. company with non-existing id)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleEntityNotFoundException(EntityNotFoundException ex, WebRequest request) {
        return SCErrorDataBuilderUtils.build(HttpStatus.NOT_FOUND, Map.of(GeneralErrorCode.ENTITY_NOT_FOUND.code(), ex.getMessage()), request);
    }


    // 400 — Validation failed (@Validated on controller method parameters - for Query params)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleValidationParamException(HandlerMethodValidationException ex, WebRequest request) {
        Map<String, String> formattedErrorMap = new HashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            String errors = result.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            formattedErrorMap.put(paramName, errors);
        });
        return SCErrorDataBuilderUtils.build(HttpStatus.BAD_REQUEST, formattedErrorMap, request);
    }

    // 400 — Illegal argument
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return SCErrorDataBuilderUtils.build(HttpStatus.BAD_REQUEST, Map.of(GeneralErrorCode.ILLEGAL_ARGUMENT.code(), ex.getMessage()), request);
    }

    // ========== MUTATION ERRORS (POST, PUT, DELETE) ==========

    // 400 — Validation failed (@Valid on @RequestBody) - ALWAYS for MUTATIONS
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> formattedErrorMap = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> formattedErrorMap.put(error.getField(), error.getDefaultMessage()));
        return SCErrorDataBuilderUtils.build(HttpStatus.BAD_REQUEST, formattedErrorMap, request);
    }

    // 400 — Malformed JSON or invalid enums/type in request body
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return SCErrorDataBuilderUtils.build(HttpStatus.BAD_REQUEST, Map.of(GeneralErrorCode.MALFORMED_JSON.code(), ex.getMostSpecificCause().getMessage()), request);
    }

    @ExceptionHandler(SCGeneralException.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleDataCorruptionException(SCGeneralException ex, WebRequest request) {
        return SCErrorDataBuilderUtils.build(ex.getStatus(),ex.toErrorMap(), request);
    }

    // ========== GENERIC FALLBACK ==========

    // 500 — Generic fallback for Query operations (use Query for safety)
    // NOTE: Controllers with Mutation endpoints should have their own Exception.class handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<SCGeneralResponseDto<Void, Void>> handleDefaultException(Exception ex, WebRequest request) {
        return SCErrorDataBuilderUtils.build(HttpStatus.INTERNAL_SERVER_ERROR, Map.of(GeneralErrorCode.SERVER_ERROR.code(),ex.getMessage()), request);
    }
}
