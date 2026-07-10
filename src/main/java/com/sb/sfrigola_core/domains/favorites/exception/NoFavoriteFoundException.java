package com.sb.sfrigola_core.domains.favorites.exception;

import com.sb.sfrigola_core.common.exception.ex.SCGeneralException;
import com.sb.sfrigola_core.domains.favorites.enums.FavoriteErrorCode;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class NoFavoriteFoundException extends SCGeneralException {
    public NoFavoriteFoundException(UUID recipePublicId) {
        super(
                HttpStatus.NOT_FOUND,
                FavoriteErrorCode.FAVORITE_NOT_FOUND,
                "No favorite found for the authenticated user on recipe with ID: " + recipePublicId
        );
    }
}
