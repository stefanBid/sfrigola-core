package com.sb.sfrigola_core.domains.ingredients.dto.input;

import com.sb.sfrigola_core.domains.ingredients.constants.IngredientValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record IngredientTranslationInputDto(
        @NotBlank(message = IngredientValidationCodeConstants.LANG_CODE_REQUIRED)
        String langCode,

        @Size(max = 150, message = IngredientValidationCodeConstants.NAME_TOO_LONG)
        String name
) implements Serializable {
}
