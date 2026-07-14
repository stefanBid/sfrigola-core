package com.sb.sfrigola_core.domains.recipes.entity;

import com.sb.sfrigola_core.common.entity.BaseEntityMinimal;
import com.sb.sfrigola_core.domains.tags.entity.Tag;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "recipe_tags")
@Getter
@Setter
public class RecipeTag extends BaseEntityMinimal {

    @EmbeddedId
    private RecipeTagId id = new RecipeTagId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("recipeId")
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}
