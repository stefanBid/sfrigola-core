package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoUserFoundException extends SCGeneralException {
    public NoUserFoundException(UUID publicId) {
        super(
                HttpStatus.NOT_FOUND,
                UserErrorCode.USER_NOT_FOUND,
                "No user found with ID: " + publicId
        );
    }
}
