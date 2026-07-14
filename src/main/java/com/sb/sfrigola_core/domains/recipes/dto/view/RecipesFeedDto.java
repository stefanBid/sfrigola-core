package com.sb.sfrigola_core.domains.recipes.dto.view;

import java.io.Serializable;
import java.util.List;

public record RecipesFeedDto(
        List<RecipeDto> recipes,
        short sortOrder
) implements Serializable {
}
