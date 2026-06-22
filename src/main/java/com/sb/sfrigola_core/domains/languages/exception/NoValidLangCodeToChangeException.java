package com.sb.sfrigola_core.domains.languages.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.languages.enums.LanguageErrorCode;
import com.sb.sfrigola_core.domains.users.enums.UserErrorCode;
import org.springframework.http.HttpStatus;

public class NoValidLangCodeToChangeException extends SCGeneralException {
    public NoValidLangCodeToChangeException(String message) {

        super(
                HttpStatus.BAD_REQUEST,
                LanguageErrorCode.INVALID_LANG_CODE,
                message);
    }
}
