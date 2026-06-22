package com.sb.sfrigola_core.common.exception.ex;

import com.sb.sfrigola_core.common.enums.GeneralErrorCode;
import org.springframework.http.HttpStatus;

public class SCDataCorruptionException extends SCGeneralException {
    public SCDataCorruptionException(String value, String field) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, GeneralErrorCode.DATA_CORRUPTED,
                "Corrupted data for field '" + field + "': " + value);
    }
}
