package com.sb.sfrigola_core.domains.recipes.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecipeTagId implements Serializable {

    private Long recipeId;
    private Long tagId;
}
