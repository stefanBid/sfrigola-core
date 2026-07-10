package com.sb.sfrigola_core.domains.stats.entity;

import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recipe_stats")
@Getter
@Setter
public class RecipeStats {

    @Id
    @Column(name = "recipe_id")
    private Long recipeId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "ratings_count", nullable = false)
    private int ratingsCount = 0;

    @Column(name = "favorites_count", nullable = false)
    private int favoritesCount = 0;

    @Column(name = "views_count", nullable = false)
    private int viewsCount = 0;

    @Column(name = "last_computed", nullable = false)
    private Instant lastComputed = Instant.now();

}
