package com.sb.sfrigola_core.domains.ingredients.dto.admin;

import java.io.Serializable;

public record IngredientTranslationDetailsAdminDto(
        String langCode,
        String langName,
        String name
) implements Serializable {
}
