package com.sb.sfrigola_core.domains.ratings.service;

import com.sb.sfrigola_core.domains.ratings.dto.input.AddRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.input.UpdateRatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RatingDto;
import com.sb.sfrigola_core.domains.ratings.dto.view.RecipeRatingStatsDto;

import java.util.UUID;

/**
 * Controller-facing contract for the ratings' domain.
 * Reads the security context internally to resolve the acting user — there is no concept of
 * managing another user's rating through this contract. Every mutation operates on the
 * authenticated user's own rating for a recipe (at most one per user per recipe, enforced by a
 * DB unique constraint on {@code (user_id, recipe_id)}).
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 */
public interface IRatingService {

    /**
     * Returns the precomputed average rating and total rating count for a recipe.
     *
     * @param recipePublicId public identifier of the recipe
     * @return the {@link RecipeRatingStatsDto} for the recipe; zero-valued if the recipe has
     *         never been rated
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     */
    RecipeRatingStatsDto getRatingStats(UUID recipePublicId);

    /**
     * Creates a new rating from the authenticated user for a recipe they have not yet rated.
     *
     * @param addRatingDto recipe to rate, score (1-5), and optional comment
     * @return the created {@link RatingDto}
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     * @throws com.sb.sfrigola_core.domains.ratings.exception.RatingAlreadyExistsException
     *         if the authenticated user has already rated this recipe
     */
    RatingDto addRating(AddRatingDto addRatingDto);

    /**
     * Edits the authenticated user's existing rating for a recipe.
     *
     * @param recipePublicId  public identifier of the recipe whose rating is being edited
     * @param updateRatingDto new score (1-5) and optional comment
     * @return the updated {@link RatingDto}
     * @throws com.sb.sfrigola_core.domains.ratings.exception.NoRatingFoundException
     *         if the authenticated user has not rated this recipe yet
     */
    RatingDto editRating(UUID recipePublicId, UpdateRatingDto updateRatingDto);
}
