package com.sb.sfrigola_core.domains.favorites.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Internal bridge contract for the favorites' domain.
 * Does NOT read the security context; all required data is received explicitly.
 * Intended for cross-domain use by the recipes domain: to populate {@code RecipeDto.isFavourite}
 * for a page of results without one query per recipe, and to back "get all my favorite recipes"
 * (owned by the recipes domain, since it is fundamentally a recipe query filtered by favorites).
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

    /**
     * Returns a page of internal recipe IDs favorited by {@code userPublicId}, most recently
     * favorited first. Used by the recipes domain to implement "get all my favorite recipes".
     *
     * @param userPublicId public ID of the user whose favorites are queried
     * @param pageable     pagination only — ordering is always most-recently-favorited first
     * @return a {@link Page} of internal recipe IDs, possibly empty
     */
    Page<Long> getFavoritedRecipeIdsPage(UUID userPublicId, Pageable pageable);
}
