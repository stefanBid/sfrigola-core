package com.sb.sfrigola_core.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

public abstract class SCGeneralException extends RuntimeException {

    @Getter
    private final HttpStatus status;
    private final String errorKey;
    private final String errorMessage;

    protected SCGeneralException(HttpStatus status, String errorKey, String
            errorMessage) {
        super(errorMessage);
        this.status = status;
        this.errorKey = errorKey;
        this.errorMessage = errorMessage;
    }

    public Map<String, String> toErrorMap() {
        return Map.of(errorKey, errorMessage);
    }
}
