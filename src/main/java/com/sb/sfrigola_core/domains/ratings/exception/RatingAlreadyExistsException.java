package com.sb.sfrigola_core.domains.ratings.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ratings.enums.RatingErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class RatingAlreadyExistsException extends SCGeneralException {
    public RatingAlreadyExistsException(UUID recipePublicId) {
        super(
                HttpStatus.CONFLICT,
                RatingErrorCode.RATING_ALREADY_EXISTS,
                "The authenticated user has already rated recipe with ID: " + recipePublicId + " — use edit instead"
        );
    }
}
