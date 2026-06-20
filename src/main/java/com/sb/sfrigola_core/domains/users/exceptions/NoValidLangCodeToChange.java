package com.sb.sfrigola_core.domains.users.exceptions;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class NoValidLangCodeToChange extends SCGeneralException {
    public NoValidLangCodeToChange(String message) {

        super(
                HttpStatus.BAD_REQUEST,
                UserErrorCode.INVALID_LANG_CODE,
                message);
    }
}
