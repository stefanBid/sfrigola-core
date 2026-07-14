package com.sb.sfrigola_core.domains.languages.exception;

import com.sb.sfrigola_core.common.enums.GeneralErrorCode;
import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import org.springframework.http.HttpStatus;

public class LocaleNotActiveException extends SCGeneralException {

    public LocaleNotActiveException(String locale) {
        this(locale, "Locale '" + locale + "' is not active or does not exist");
    }

    public LocaleNotActiveException(String locale, String message) {
        super(HttpStatus.BAD_REQUEST, GeneralErrorCode.LOCALE_NOT_ACTIVE, message);
    }
}
