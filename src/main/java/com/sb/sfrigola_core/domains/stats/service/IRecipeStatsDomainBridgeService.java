package com.sb.sfrigola_core.domains.stats.service;

import com.sb.sfrigola_core.domains.recipes.entity.Recipe;
import com.sb.sfrigola_core.domains.stats.entity.RecipeStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal bridge contract for the stats' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * No controller is exposed for this domain (see CLAUDE.md) — it is only ever called by the
 * favorites and ratings domains to keep {@code recipe_stats} in sync, and by the recipes domain
 * to read it — this is the single point where recipe statistics are read; nothing else should
 * query {@code recipe_stats} directly.
 * Write methods follow the "succeed or throw" contract — no boolean return, exception on failure.
 * The underlying {@code recipe_stats} row is created lazily (upserted) on first write for a
 * recipe, since recipe creation itself does not seed one.
 */
public interface IRecipeStatsDomainBridgeService {

    /**
     * Returns the current precomputed aggregates for {@code recipeId}.
     *
     * @param recipeId internal ID of the recipe whose stats are read
     * @return the {@link RecipeStats} entity for the recipe, or {@link Optional#empty()}
     *         if no stats row exists yet (i.e. the recipe has never been favorited or rated)
     */
    Optional<RecipeStats> getStats(Long recipeId);

    /**
     * Returns the top 10 recipes ranked by virality (most favorited first), for the VIRAL home
     * feed row.
     *
     * @param categoryIds internal category IDs to restrict to (as resolved by the recipes domain's
     *                     {@code resolveCategoryFilterIds}); {@code null} means no restriction
     * @return recipe ID → {@link RecipeStats}, at most 10 entries, ordered by {@code favoritesCount}
     *         descending; never {@code null}
     */
    Map<Long, RecipeStats> getPreviewByStats(List<Long> categoryIds);

    /**
     * Returns a full page of published recipe IDs ranked by virality (most favorited first), for
     * the paginated VIRAL feed row (unlike {@link #getPreviewByStats}, which is capped at a fixed
     * top-10 for the home-feed preview). Recipes with no {@code recipe_stats} row (never favorited
     * or rated) are excluded entirely rather than sorted last.
     *
     * @param pageable page/size to fetch; any sort it carries is ignored, ordering is always by
     *                 {@code favoritesCount} descending
     * @return a {@link Page} of recipe IDs, ordered by {@code favoritesCount} descending; never {@code null}
     */
    Page<Long> getPublishedRecipeIdsOrderByFavoritesDesc(Pageable pageable);

    /**
     * Batch variant of {@link #getStats}: returns the stats row for every recipe in
     * {@code recipeIds} that already has one. Recipes with no row (never favorited or
     * rated) are simply absent from the map — callers must default the aggregates themselves
     * (e.g. zero rating/count). Used by the recipes domain to localize a page of results
     * without one query per recipe.
     *
     * @param recipeIds internal recipe IDs to look up; never {@code null}
     * @return recipe ID → {@link RecipeStats}, only for recipes that have a stats row; never {@code null}
     */
    Map<Long, RecipeStats> getStatsBatch(List<Long> recipeIds);

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
