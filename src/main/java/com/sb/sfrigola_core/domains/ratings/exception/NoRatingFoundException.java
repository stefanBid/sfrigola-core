package com.sb.sfrigola_core.domains.ratings.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.ratings.enums.RatingErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoRatingFoundException extends SCGeneralException {
    public NoRatingFoundException(UUID recipePublicId) {
        super(
                HttpStatus.NOT_FOUND,
                RatingErrorCode.RATING_NOT_FOUND,
                "No rating found for the authenticated user on recipe with ID: " + recipePublicId
        );
    }
}
