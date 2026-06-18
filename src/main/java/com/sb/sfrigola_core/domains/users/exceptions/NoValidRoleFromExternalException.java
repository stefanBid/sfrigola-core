package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.SCGeneralException;
import org.springframework.http.HttpStatus;

public class NoValidRoleFromExternalException extends SCGeneralException {
    public NoValidRoleFromExternalException(String message) {
        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "invalid_data_from_external",
                message
        );
    }
}
