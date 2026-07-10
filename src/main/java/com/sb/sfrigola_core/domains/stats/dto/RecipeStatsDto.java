package com.sb.sfrigola_core.domains.stats.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record RecipeStatsDto(
        BigDecimal avgRating,
        int ratingsCount,
        int favoritesCount,
        int viewsCount
) implements Serializable {

    public static RecipeStatsDto empty() {
        return new RecipeStatsDto(BigDecimal.ZERO, 0, 0, 0);
    }
}
