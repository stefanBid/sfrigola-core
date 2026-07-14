package com.sb.sfrigola_core.domains.ratings.dto.view;

import java.io.Serializable;
import java.util.UUID;

public record RatingDto(
        UUID publicId,
        UUID recipePublicId,
        Short score,
        String comment
) implements Serializable {
}
