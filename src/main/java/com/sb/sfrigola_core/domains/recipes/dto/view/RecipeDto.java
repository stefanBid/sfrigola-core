package com.sb.sfrigola_core.domains.recipes.dto.view;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record RecipeDto(
        UUID publicId,
        UUID authorPublicId,
        UUID categoryPublicId,
        String specificTranslationTitle,
        String specificTranslationDescription,
        BigDecimal avgRating,
        boolean isFavourite,
        int totalTimeMin,
        BigDecimal economicalRatio
) implements Serializable {
}
