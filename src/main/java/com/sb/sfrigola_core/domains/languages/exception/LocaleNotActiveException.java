package com.sb.sfrigola_core.domains.languages.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.languages.enums.LanguageErrorCode;
import org.springframework.http.HttpStatus;

public class LocaleNotActiveException extends SCGeneralException {
    public LocaleNotActiveException(String locale) {
        super(
                HttpStatus.BAD_REQUEST,
                LanguageErrorCode.LOCALE_NOT_ACTIVE,
                "Locale '" + locale + "' does not exist or is not active"
        );
    }
}
