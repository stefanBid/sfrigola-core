package com.sb.sfrigola_core.common.exception.ex;

import com.sb.sfrigola_core.common.enums.GeneralErrorCode;
import org.springframework.http.HttpStatus;

public class SCFileStorageException extends SCGeneralException {
    public SCFileStorageException(String operation, String fileName) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, GeneralErrorCode.FILE_STORAGE_ERROR,
                "Failed to " + operation + " file: " + fileName);
    }

    public SCFileStorageException(String operation, String fileName, String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, GeneralErrorCode.FILE_STORAGE_ERROR,
                "Failed to " + operation + " file '" + fileName + "': " + reason);
    }
}
