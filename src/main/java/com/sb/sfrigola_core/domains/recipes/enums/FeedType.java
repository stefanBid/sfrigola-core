package com.sb.sfrigola_core.domains.recipes.enums;

import com.sb.sfrigola_core.common.exception.ex.SCEnumValidationException;

import java.util.Arrays;

public enum FeedType {
    QUICK,        // sort prepTimeMin+cookTimeMin asc — implementabile subito
    LIKE_A_CHEF,      // difficulty=hard / tempo alto — implementabile subito
    ECONOMICAL,   // ratio ingredienti/servings asc — implementabile subito
    VIRAL;        // sort recipe_stats (favorites/rating/views) desc — BLOCCATO, serve stats domain

    public static FeedType fromString(String feedType) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(feedType))
                .findFirst()
                .orElseThrow(() -> new SCEnumValidationException(FeedType.class.getSimpleName(), feedType, values()));
    }
}
