package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class NoChangeRoleToAdminException extends SCGeneralException {
    public NoChangeRoleToAdminException(String message) {
        super(
                HttpStatus.FORBIDDEN,
                UserErrorCode.CANNOT_CHANGE_ROLE_TO_ADMIN,
                message
        );
    }
}
