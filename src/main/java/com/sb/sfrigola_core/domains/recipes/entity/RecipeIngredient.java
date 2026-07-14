package com.sb.sfrigola_core.domains.recipes.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.domains.ingredients.entity.Ingredient;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "recipe_ingredients",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recipe_id", "ingredient_id"})
)
@Getter
@Setter
public class RecipeIngredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(name = "quantity", precision = 8, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "preparation_note", length = 200)
    private String preparationNote;

    @Column(name = "sort_order", nullable = false)
    private Short sortOrder = 0;

}
