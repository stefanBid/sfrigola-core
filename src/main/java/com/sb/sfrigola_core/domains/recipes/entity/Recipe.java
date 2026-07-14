package com.sb.sfrigola_core.domains.recipes.entity;

import com.sb.sfrigola_core.common.entity.BaseEntity;
import com.sb.sfrigola_core.domains.categories.entity.Category;
import com.sb.sfrigola_core.domains.recipes.enums.DifficultyLevel;
import com.sb.sfrigola_core.domains.recipes.enums.MealType;
import com.sb.sfrigola_core.domains.recipes.enums.SeasonType;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recipes")
@Getter
@Setter
public class Recipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private SCUser author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "difficulty", nullable = false, columnDefinition = "difficulty_level")
    @ColumnTransformer(write = "?::difficulty_level")
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Column(name = "meal_type", columnDefinition = "meal_type")
    @ColumnTransformer(write = "?::meal_type")
    private MealType mealType;

    @Column(name = "season", nullable = false, columnDefinition = "season_type")
    @ColumnTransformer(write = "?::season_type")
    private SeasonType season = SeasonType.ALL_YEAR;

    @Column(name = "prep_time_min")
    private Integer prepTimeMin;

    @Column(name = "cook_time_min")
    private Integer cookTimeMin;

    @Column(name = "servings")
    private Short servings;

    @Column(name = "is_vegetarian", nullable = false)
    private boolean isVegetarian = false;

    @Column(name = "is_vegan", nullable = false)
    private boolean isVegan = false;

    @Column(name = "is_gluten_free", nullable = false)
    private boolean isGlutenFree = false;

    @Column(name = "is_published", nullable = false)
    private boolean isPublished = false;

    // Materialized in the service layer on create/update (no DB trigger) — kept in sync
    // with prepTimeMin/cookTimeMin/servings/recipeIngredients whenever any of them change.
    @Column(name = "total_time_min", nullable = false)
    private int totalTimeMin = 0;

    @Column(name = "economical_ratio", nullable = false)
    private BigDecimal economicalRatio = BigDecimal.ZERO;

    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeTranslation> translations = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeTag> recipeTags = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

}
