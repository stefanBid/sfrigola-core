package com.sb.sfrigola_core.domains.ingredients.entity;

import com.sb.sfrigola_core.common.entity.BaseEntityMinimal;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ingredient_tags")
@Getter
@Setter
public class IngredientTag extends BaseEntityMinimal {

    @EmbeddedId
    private IngredientTagId id = new IngredientTagId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ingredientId")
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
