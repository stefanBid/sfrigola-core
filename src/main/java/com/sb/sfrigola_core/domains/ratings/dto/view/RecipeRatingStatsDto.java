package com.sb.sfrigola_core.domains.ratings.dto.view;

import java.io.Serializable;
import java.math.BigDecimal;

public record RecipeRatingStatsDto(
        BigDecimal averageRating,
        int totalRatings
) implements Serializable {
}
