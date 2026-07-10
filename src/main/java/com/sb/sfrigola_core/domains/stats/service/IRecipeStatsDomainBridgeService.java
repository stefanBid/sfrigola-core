package com.sb.sfrigola_core.domains.stats.service;

import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.stats.dto.RecipeStatsDto;

/**
 * Internal bridge contract for the stats' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * No controller is exposed for this domain (see CLAUDE.md) — it is only ever called by the
 * favorites and ratings domains to keep {@code recipe_stats} in sync, and by them alone.
 * Write methods follow the "succeed or throw" contract — no boolean return, exception on failure.
 * The underlying {@code recipe_stats} row is created lazily (upserted) on first write for a
 * recipe, since recipe creation itself does not seed one.
 */
public interface IRecipeStatsDomainBridgeService {

    /**
     * Returns the current precomputed aggregates for {@code recipeId}.
     *
     * @param recipeId internal ID of the recipe whose stats are read
     * @return the {@link RecipeStatsDto} for the recipe, or {@link RecipeStatsDto#empty()}
     *         if no stats row exists yet (i.e. the recipe has never been favorited or rated)
     */
    RecipeStatsDto getStats(Long recipeId);

    /**
     * Records that {@code recipe} was added to a user's favorites: increments {@code favorites_count}.
     *
     * @param recipe the favorited recipe; never {@code null}
     */
    void registerFavoriteAdded(Recipe recipe);

    /**
     * Records that {@code recipe} was removed from a user's favorites: decrements
     * {@code favorites_count}, floored at zero.
     *
     * @param recipe the un-favorited recipe; never {@code null}
     */
    void registerFavoriteRemoved(Recipe recipe);

    /**
     * Records a brand-new rating for {@code recipe}: increments {@code ratings_count} and
     * recomputes {@code avg_rating} to include {@code score}.
     *
     * @param recipe the rated recipe; never {@code null}
     * @param score  the new rating's score, between 1 and 5
     */
    void registerRatingAdded(Recipe recipe, short score);

    /**
     * Records a change to an existing rating's score for {@code recipe}: recomputes
     * {@code avg_rating} by replacing {@code oldScore} with {@code newScore}; {@code ratings_count}
     * is unchanged.
     *
     * @param recipe   the recipe whose rating changed; never {@code null}
     * @param oldScore the rating's previous score, between 1 and 5
     * @param newScore the rating's new score, between 1 and 5
     */
    void registerRatingScoreChanged(Recipe recipe, short oldScore, short newScore);
}
