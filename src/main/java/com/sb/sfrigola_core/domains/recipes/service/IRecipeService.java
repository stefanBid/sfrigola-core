package com.sb.sfrigola_core.domains.recipes.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;
import com.sb.sfrigola_core.domains.recipes.dto.input.AddRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.UpdateRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDetailsAdminDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipePreviewAdminDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipesFeedDto;
import com.sb.sfrigola_core.domains.recipes.enums.FeedType;
import com.sb.sfrigola_core.domains.recipes.models.RecipeSpecificFilter;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.UUID;

/**
 * Controller-facing contract for the recipes' domain.
 * Reads the security context internally to resolve the acting user as the recipe's author
 * on creation, and to enforce ownership (author-or-admin) on update and delete.
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 */
public interface IRecipeService {

    /**
     * Returns a paginated list of published recipes localized for the requested locale.
     * Only recipes that are {@code isPublished = true} and have a translation for {@code locale}
     * are included — draft recipes are never visible through this method.
     *
     * @param filterQuery pagination and sorting parameters, with optional {@code searchKey}
     *                     matched case-insensitively against the translated title
     * @param locale      BCP-47 language code used to filter and localize results
     * @return a {@link SCPagedResult} of {@link RecipeDto}; never {@code null}
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<RecipeDto> getAll(SCFilterQuery<Void> filterQuery, @NonNull String locale);

    /**
     * Returns the home-page feed for a selected category: a fixed set of short, unpaginated
     * recipe rows ({@link RecipesFeedDto#recipes()}, capped at a small top-N per row), one row
     * per {@link FeedType}, each with its own ranking criterion (e.g. quickest to prepare,
     * most economical by ingredient-count-to-servings ratio). Only published recipes with a
     * translation for {@code locale} are considered.
     * <p>
     * Only {@link FeedType}s that are currently implementable are present as keys in the
     * returned map — feed types that depend on domains not yet built (ratings/favorites stats)
     * are omitted entirely rather than returned with an empty row, until those domains exist.
     * {@link RecipesFeedDto#sortOrder()} indicates the display order of that row on the home page.
     *
     * @param categoryId public identifier of the category whose recipes populate the feed
     * @param locale     BCP-47 language code used to filter and localize results
     * @return a {@link Map} keyed by {@link FeedType}, never {@code null}; never contains
     *         a {@code null} key or value, but may omit keys for not-yet-implemented feed types
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given public ID
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    Map<FeedType, RecipesFeedDto> getAllHomeFeed(@NonNull UUID categoryId, @NonNull String locale);

    /**
     * Returns a paginated admin preview of ALL recipes (published and draft), including
     * localization coverage counts (present and missing) to support CMS overviews.
     * Only recipes that have a translation for {@code locale} are returned; the preview uses
     * that specific translation.
     *
     * @param filterQuery pagination, sorting, optional search key, and optional
     *                     {@link RecipeSpecificFilter} (difficulty, meal type, season, dietary flags, publish status)
     * @param locale      BCP-47 language code used to filter and select the preview translation; never {@code null}
     * @return a {@link SCPagedResult} of {@link RecipePreviewAdminDto}; never {@code null}
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<RecipePreviewAdminDto> getAllAdmin(SCFilterQuery<RecipeSpecificFilter> filterQuery, @NonNull String locale);

    /**
     * Returns full admin details for a single recipe (published or draft), including its
     * ingredient list, tag list, and a preview of the translation for the requested locale.
     * <p>
     * {@code locale} is mandatory and never filters — it only selects which translation is
     * returned. If no translation exists for {@code locale}, the translation preview fields are {@code null}.
     *
     * @param publicId public identifier of the recipe
     * @param locale   BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return {@link RecipeDetailsAdminDto} with the translation preview, ingredients and tags
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     */
    RecipeDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale);

    /**
     * Creates a new recipe authored by the currently authenticated user (resolved from the
     * security context — never trusted from the payload). Translations provided in {@code addRecipeDto}
     * must cover every active language and are inserted as-is; the ingredient list and tag list
     * are inserted as given, no merge occurs on create.
     *
     * @param addRecipeDto creation payload — category, difficulty/meal/season, timing, dietary flags,
     *                     publish flag, ingredient lines, tag IDs and at least one translation
     * @param locale       BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return admin preview of the newly created recipe
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code addRecipeDto.categoryPublicId()} is non-{@code null} but no matching category exists
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if any ingredient ID in {@code addRecipeDto.ingredients()} does not match an existing ingredient
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException
     *         if any tag ID in {@code addRecipeDto.recipeTagsIds()} does not match an existing tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagScopeNotAllowedException
     *         if any resolved tag has scope {@code ingredient} (not usable on recipes)
     * @throws com.sb.sfrigola_core.domains.recipes.exception.DuplicateRecipeLocaleException
     *         if the same locale appears more than once in {@code addRecipeDto.translations()}
     * @throws LocaleNotActiveException
     *         if a translation references a language that is not active (thrown by the languages domain bridge)
     * @throws com.sb.sfrigola_core.domains.recipes.exception.MissingRecipeLocalesException
     *         if {@code addRecipeDto.translations()} does not cover all active languages
     */
    RecipePreviewAdminDto createRecipe(AddRecipeDto addRecipeDto, @NonNull String locale);

    /**
     * Updates an existing recipe's category, difficulty/meal/season, timing, dietary flags,
     * publish flag, ingredient list, tag list, and exactly one translation:
     * {@code updateRecipeDto.specificTranslation()} either edits the translation for that locale
     * if one already exists, or adds a new one. Only one locale can be touched per call — to
     * update multiple translations, call this method once per locale.
     * <p>
     * Only the recipe's author or an admin may update it — checked against the security context.
     *
     * @param publicId public identifier of the recipe to update
     * @param updateRecipeDto new category, difficulty/meal/season, timing, dietary flags, publish flag,
     *                        ingredient lines, tag IDs, and the single translation to upsert
     * @return admin preview of the updated recipe, previewing the upserted translation
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     * @throws com.sb.sfrigola_core.domains.recipes.exception.RecipeAuthorMismatchException
     *         if the authenticated user is neither the recipe's author nor an admin
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code updateRecipeDto.categoryPublicId()} is non-{@code null} but no matching category exists
     * @throws com.sb.sfrigola_core.domains.ingredients.exception.NoIngredientFoundException
     *         if any ingredient ID in {@code updateRecipeDto.ingredients()} does not match an existing ingredient
     * @throws com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException
     *         if any tag ID in {@code updateRecipeDto.recipeTagsIds()} does not match an existing tag
     * @throws com.sb.sfrigola_core.domains.tags.exception.TagScopeNotAllowedException
     *         if any resolved tag has scope {@code ingredient} (not usable on recipes)
     * @throws LocaleNotActiveException
     *         if {@code updateRecipeDto.specificTranslation()} references a language that is not active
     */
    RecipePreviewAdminDto updateRecipe(UUID publicId, UpdateRecipeDto updateRecipeDto);

    /**
     * Deletes a recipe and all of its translations, ingredient lines and tag associations (cascade).
     * Only the recipe's author or an admin may delete it — checked against the security context.
     * No preview DTO is returned — the recipe no longer exists, so only its identifier is handed back.
     *
     * @param publicId public identifier of the recipe to delete
     * @return the {@code publicId} of the deleted recipe
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     * @throws com.sb.sfrigola_core.domains.recipes.exception.RecipeAuthorMismatchException
     *         if the authenticated user is neither the recipe's author nor an admin
     */
    UUID deleteRecipe(UUID publicId);

}
