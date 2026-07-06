package com.sb.sfrigola_core.domains.ingredients.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IngredientTagId implements Serializable {

    private Long ingredientId;
    private Long tagId;
}
