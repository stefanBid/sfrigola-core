package com.sb.sfrigola_core.domains.recipes.dto.input;

import com.sb.sfrigola_core.domains.recipes.constants.RecipeValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public record RecipeTranslationInputDto(
        @NotBlank(message = RecipeValidationCodeConstants.LANG_CODE_REQUIRED)
        String langCode,

        @NotBlank(message = RecipeValidationCodeConstants.TITLE_REQUIRED)
        @Size(max = 200, message = RecipeValidationCodeConstants.TITLE_TOO_LONG)
        String title,

        String description,

        @NotBlank(message = RecipeValidationCodeConstants.INSTRUCTIONS_REQUIRED)
        String instructions
) implements Serializable {
}
