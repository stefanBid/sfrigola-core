package com.sb.sfrigola_core.domains.recipes.enums;

import java.util.Arrays;

public enum FeedType {
    QUICK,        // sort prepTimeMin+cookTimeMin asc — implementabile subito
    LIKE_A_CHEF,      // difficulty=hard / tempo alto — implementabile subito
    ECONOMICAL,   // ratio ingredienti/servings asc — implementabile subito
    VIRAL,        // sort recipe_stats (favorites/rating/views) desc — BLOCCATO, serve stats domain
    FAVOURITE;    // filtro su favorites dell'utente autenticato — BLOCCATO, serve favorites domain

    public static FeedType fromString(String feedType) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(feedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid feed type: " + feedType + ". It has to be one of: " + Arrays.toString(values())));
    }
}
