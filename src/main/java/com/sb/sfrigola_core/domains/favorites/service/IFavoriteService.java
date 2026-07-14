package com.sb.sfrigola_core.domains.favorites.service;

import java.util.UUID;

/**
 * Controller-facing contract for the favorites' domain.
 * Reads the security context internally to resolve the acting user — there is no concept of
 * managing another user's favorites through this contract. Every method operates on the
 * authenticated user's own favorites list.
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 * <p>
 * Listing the authenticated user's favorited recipes is exposed by the recipes domain instead
 * (see {@code IRecipeService#getAllMyFavoriteRecipes}) — it is fundamentally a recipe query
 * filtered by favorites, and returns {@code RecipeDto}, which this domain has no other reason to
 * depend on.
 */
public interface IFavoriteService {

    /**
     * Adds the given recipe to the authenticated user's favorites.
     *
     * @param recipePublicId public identifier of the recipe to favorite
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     * @throws com.sb.sfrigola_core.domains.favorites.exception.FavoriteAlreadyExistsException
     *         if the authenticated user has already favorited this recipe
     */
    void addFavorite(UUID recipePublicId);

    /**
     * Removes the given recipe from the authenticated user's favorites.
     *
     * @param recipePublicId public identifier of the recipe to un-favorite
     * @throws com.sb.sfrigola_core.domains.favorites.exception.NoFavoriteFoundException
     *         if the authenticated user has not favorited this recipe
     */
    void removeFavorite(UUID recipePublicId);
}
