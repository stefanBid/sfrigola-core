package com.sb.sfrigola_core.domains.favorites.service;

import java.util.List;
import java.util.Set;

/**
 * Internal bridge contract for the favorites' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by the recipes domain, to populate {@code RecipeDto.isFavourite}
 * for a page of results without one query per recipe.
 */
public interface IFavoriteDomainBridgeService {

    /**
     * Checks whether {@code recipeId} is one of {@code userId}'s favorites.
     *
     * @param userId   internal ID of the user
     * @param recipeId internal ID of the recipe
     * @return {@code true} if a favorite row exists for that user/recipe pair
     */
    boolean isFavoritedByUser(Long userId, Long recipeId);

    /**
     * Batch variant of {@link #isFavoritedByUser}: returns the subset of {@code recipeIds}
     * that {@code userId} has favorited.
     *
     * @param userId    internal ID of the user
     * @param recipeIds internal IDs of the recipes to check; never {@code null}
     * @return the subset of {@code recipeIds} favorited by {@code userId}; never {@code null},
     *         empty if {@code recipeIds} is empty or none are favorited
     */
    Set<Long> getFavoritedRecipeIds(Long userId, List<Long> recipeIds);
}
