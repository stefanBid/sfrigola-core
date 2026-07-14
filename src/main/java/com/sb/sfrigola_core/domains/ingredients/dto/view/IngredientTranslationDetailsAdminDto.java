package com.sb.sfrigola_core.domains.ingredients.dto.view;

import java.io.Serializable;

public record IngredientTranslationDetailsAdminDto(
        String langCode,
        String langName,
        String name
) implements Serializable {
}
