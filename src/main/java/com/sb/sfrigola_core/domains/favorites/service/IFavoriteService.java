package com.sb.sfrigola_core.domains.favorites.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDto;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Controller-facing contract for the favorites' domain.
 * Reads the security context internally to resolve the acting user — there is no concept of
 * managing another user's favorites through this contract. Every method operates on the
 * authenticated user's own favorites list.
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 */
public interface IFavoriteService {

    /**
     * Returns a paginated list of the authenticated user's favorited recipes, most recently
     * favorited first, localized to {@code locale}.
     *
     * @param filterQuery pagination only — no search/sort is supported
     * @param locale      BCP-47 language code used to localize each recipe's title/description
     * @return a {@link SCPagedResult} of {@link RecipeDto}; never {@code null}
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<RecipeDto> getAllMyFavorites(SCFilterQuery<Void> filterQuery, @NonNull String locale);

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
