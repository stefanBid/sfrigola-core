package com.sb.sfrigola_core.domains.recipes.dto.input;

import com.sb.sfrigola_core.domains.recipes.constants.RecipeValidationCodeConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

public record RecipeTranslationInputDto(
        @NotBlank(message = RecipeValidationCodeConstants.LANG_CODE_REQUIRED)
        String langCode,

        @NotBlank(message = RecipeValidationCodeConstants.TITLE_REQUIRED)
        @Size(max = 200, message = RecipeValidationCodeConstants.TITLE_TOO_LONG)
        String title,

        String description,

        @NotEmpty(message = RecipeValidationCodeConstants.INSTRUCTIONS_MIN_ONE)
        List<@NotBlank(message = RecipeValidationCodeConstants.INSTRUCTIONS_STEP_BLANK) String> instructions
) implements Serializable {
}
