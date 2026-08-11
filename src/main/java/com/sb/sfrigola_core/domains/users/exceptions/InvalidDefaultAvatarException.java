package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class InvalidDefaultAvatarException extends SCGeneralException {
    public InvalidDefaultAvatarException(String avatarKey) {
        super(
                HttpStatus.BAD_REQUEST,
                UserErrorCode.INVALID_DEFAULT_AVATAR,
                "'" + avatarKey + "' is not a valid default avatar"
        );
    }
}
