package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class SCCanNotActiveOrDeactivateYourselfException extends SCGeneralException {
    public SCCanNotActiveOrDeactivateYourselfException(String message) {
        super(
                HttpStatus.FORBIDDEN,
                UserErrorCode.CANNOT_CHANGE_OWN_ACTIVE_STATUS,
                message
        );
    }
}
