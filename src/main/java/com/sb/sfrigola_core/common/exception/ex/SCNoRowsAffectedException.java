package com.sb.sfrigola_core.common.exception.ex;

import com.sb.sfrigola_core.common.enums.GeneralErrorCode;
import org.springframework.http.HttpStatus;

public class SCNoRowsAffectedException extends SCGeneralException {
    public SCNoRowsAffectedException(String message) {

        super(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GeneralErrorCode.NO_ROWS_AFFECTED,
                message
        );
    }
}
