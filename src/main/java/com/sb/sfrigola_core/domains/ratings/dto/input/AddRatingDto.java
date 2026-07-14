package com.sb.sfrigola_core.domains.ratings.dto.input;

import com.sb.sfrigola_core.domains.ratings.constants.RatingValidationCodeConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.UUID;

public record AddRatingDto(
        @NotNull(message = RatingValidationCodeConstants.RECIPE_ID_REQUIRED)
        UUID recipePublicId,

        @NotNull(message = RatingValidationCodeConstants.SCORE_REQUIRED)
        @Min(value = 1, message = RatingValidationCodeConstants.SCORE_MUST_BE_AT_LEAST_ONE)
        @Max(value = 5, message = RatingValidationCodeConstants.SCORE_MUST_BE_AT_MOST_FIVE)
        Short score,

        @Size(max = 2000, message = RatingValidationCodeConstants.COMMENT_TOO_LONG)
        String comment
) implements Serializable {
}
