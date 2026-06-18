package com.sb.sfrigola_core.common.exception;

import org.springframework.http.HttpStatus;

public class DataCorruptionException extends SCGeneralException {
    public DataCorruptionException(String value, String field) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "data_corruption",
                "Corrupted data for field '" + field + "': " + value);
    }
}
