package com.sb.sfrigola_core.domains.ratings.service;

import com.sb.sfrigola_core.domains.ratings.dto.view.RecipeRatingStatsDto;

/**
 * Internal bridge contract for the ratings' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by the recipes domain, to display a recipe's average rating
 * and rating count without depending on the stats domain directly — ratings and favorites are
 * the only domains that talk to stats.
 */
public interface IRatingDomainBridgeService {

    /**
     * Returns the precomputed average rating and total rating count for a recipe.
     *
     * @param recipeId internal ID of the recipe
     * @return the {@link RecipeRatingStatsDto} for the recipe; zero-valued if the recipe has
     *         never been rated
     */
    RecipeRatingStatsDto getRatingStats(Long recipeId);
}
