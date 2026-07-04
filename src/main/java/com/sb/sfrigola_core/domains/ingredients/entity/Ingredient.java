package com.sb.sfrigola_core.domains.ingredients.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ingredients")
@Getter
@Setter
public class Ingredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "calories_per_100g", precision = 7, scale = 2)
    private BigDecimal caloriesPer100g;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allergens", columnDefinition = "text[]")
    private String[] allergens;

    @Column(name = "is_vegetarian", nullable = false)
    private boolean isVegetarian = false;

    @Column(name = "is_vegan", nullable = false)
    private boolean isVegan = false;

    @Column(name = "is_gluten_free", nullable = false)
    private boolean isGlutenFree = false;

    @OneToMany(mappedBy = "ingredient", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "ingredient", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientTag> ingredientTags = new ArrayList<>();

}
