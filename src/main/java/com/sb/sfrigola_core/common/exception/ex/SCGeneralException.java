package com.sb.sfrigola_core.common.exception.ex;

import com.sb.sfrigola_core.common.exception.ISCErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

public abstract class SCGeneralException extends RuntimeException {

    @Getter
    private final HttpStatus status;
    private final ISCErrorCode errorCode;
    private final String errorMessage;

    protected SCGeneralException(HttpStatus status, ISCErrorCode errorCode, String
            errorMessage) {
        super(errorMessage);
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public Map<String, String> toErrorMap() {
        return Map.of(errorCode.code(), errorMessage);
    }
}
