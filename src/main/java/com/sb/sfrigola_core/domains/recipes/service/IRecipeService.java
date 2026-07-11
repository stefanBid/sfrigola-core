package com.sb.sfrigola_core.domains.recipes.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException;
import com.sb.sfrigola_core.domains.recipes.dto.input.AddRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.input.UpdateRecipeDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDetailsAdminDto;
import com.sb.sfrigola_core.domains.recipes.dto.view.RecipeDetailsDto;
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
     * @param categoryId  optional public identifier of the category to filter by; {@code null} means no category filter
     * @param locale      BCP-47 language code used to filter and select the preview translation; never {@code null}
     * @return a {@link SCPagedResult} of {@link RecipePreviewAdminDto}; never {@code null}
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code categoryId} is non-{@code null} but no matching category exists
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<RecipePreviewAdminDto> getAllAdmin(SCFilterQuery<RecipeSpecificFilter> filterQuery, UUID categoryId, @NonNull String locale);

    /**
     * Returns a paginated list of published recipes whose translated title and/or description
     * match {@code filterQuery.searchKey()} (case-insensitive substring match), optionally
     * restricted to one category. Only recipes with a translation for {@code locale} are
     * included — draft recipes are never visible through this method.
     *
     * @param filterQuery pagination, with optional {@code searchKey} matched against the
     *                    translated title and/or description; sorting/other filters are ignored
     * @param categoryId  optional public identifier of the category to filter by; {@code null} means no category filter
     * @param locale      BCP-47 language code used to filter and localize results
     * @return a {@link SCPagedResult} of {@link RecipeDto}; never {@code null}
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code categoryId} is non-{@code null} but no matching category exists
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<RecipeDto> searchRecipes(SCFilterQuery<Void> filterQuery, UUID categoryId, @NonNull String locale);

    /**
     * Returns a paginated list of the authenticated user's favorited recipes, most recently
     * favorited first, localized to {@code locale}. Lives here rather than in the favorites
     * domain because it is fundamentally a recipe query (filtered by favorites) and returns
     * {@link RecipeDto} — the favorites domain has no other reason to depend on it.
     *
     * @param filterQuery pagination only — no search/sort is supported
     * @param locale      BCP-47 language code used to localize each recipe's title/description
     * @return a {@link SCPagedResult} of {@link RecipeDto}; never {@code null}
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    SCPagedResult<RecipeDto> getAllMyFavoriteRecipes(SCFilterQuery<Void> filterQuery, @NonNull String locale);

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
     * Returns public details for a single published recipe, including its ingredient list,
     * tag list, and a preview of the translation for the requested locale. Unlike
     * {@link #getByPublicIdAdmin}, draft recipes ({@code isPublished = false}) are never
     * returned — they surface as {@link com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException}
     * exactly like a missing recipe, so drafts are never distinguishable from non-existence.
     * <p>
     * {@code locale} is mandatory and never filters — it only selects which translation is
     * returned. If no translation exists for {@code locale}, the translation preview fields are {@code null}.
     *
     * @param publicId public identifier of the recipe
     * @param locale   BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return {@link RecipeDetailsDto} with the translation preview, ingredients and tags
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no published recipe exists with the given public ID
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    RecipeDetailsDto getByPublicId(UUID publicId, @NonNull String locale);

    /**
     * Creates a new recipe authored by the currently authenticated user (resolved from the
     * security context — never trusted from the payload). Translation coverage requirements and
     * the initial {@code isPublished} value differ by the actor's role:
     * <ul>
     *     <li><b>Contributor</b>: exactly one translation, in any active language of the
     *     contributor's choosing — {@code locale} plays no role in this choice, it only selects
     *     which translation is shown in the returned preview, same as everywhere else. The
     *     recipe is created with {@code isPublished = false}; only an admin can publish it
     *     (a separate admin-only action), typically after adding the missing active-language
     *     translations via {@link #updateRecipe} (one locale per call).</li>
     *     <li><b>Admin</b>: translations must cover every active language, no more no less —
     *     same rule as every other translatable domain's create. The recipe is created with
     *     {@code isPublished = true} immediately, since all languages are already covered.</li>
     * </ul>
     * The ingredient list and tag list are inserted as given, no merge occurs on create.
     *
     * @param addRecipeDto creation payload — category, difficulty/meal/season, timing, dietary flags,
     *                     ingredient lines, tag IDs, and translations (contributor: exactly one, any
     *                     active language; admin: one per active language)
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
     * @throws com.sb.sfrigola_core.domains.recipes.exception.ContributorTranslationLimitExceededException
     *         if the actor is a contributor and {@code addRecipeDto.translations()} has more than one entry
     * @throws com.sb.sfrigola_core.domains.recipes.exception.DuplicateRecipeLocaleException
     *         if the actor is an admin and the same locale appears more than once in {@code addRecipeDto.translations()}
     * @throws LocaleNotActiveException
     *         if a translation references a language that is not active (thrown by the languages domain bridge)
     * @throws com.sb.sfrigola_core.domains.recipes.exception.MissingRecipeLocalesException
     *         if the actor is an admin and {@code addRecipeDto.translations()} does not cover all active languages
     */
    RecipePreviewAdminDto createRecipe(AddRecipeDto addRecipeDto, @NonNull String locale);

    /**
     * Updates an existing recipe's category, difficulty/meal/season, timing, dietary flags,
     * ingredient list, tag list, and exactly one translation:
     * {@code updateRecipeDto.specificTranslation()} either edits the translation for that locale
     * if one already exists, or adds a new one. Only one locale can be touched per call — to
     * update multiple translations, call this method once per locale.
     * <p>
     * Only the recipe's author or an admin may update it — checked against the security context.
     * If the actor is not an admin (i.e. the recipe's own contributor-author), the recipe is
     * reverted to {@code isPublished = false} regardless of its previous value — any content
     * change by the contributor needs re-approval; an admin's edit does not trigger this, whatever
     * {@code isPublished} currently is is left untouched. Publishing/unpublishing itself is a
     * separate admin-only action, not part of this method.
     *
     * @param publicId public identifier of the recipe to update
     * @param updateRecipeDto new category, difficulty/meal/season, timing, dietary flags,
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
     * Publishes a recipe (sets {@code isPublished = true}), making it visible via public
     * search/details/home feed. Admin-only — unlike {@link #updateRecipe}, there is no
     * author-or-admin fallback: a contributor cannot self-publish even their own recipe, not
     * checked here but enforced by the security path. Intended flow: a contributor creates a
     * recipe with a translation for their own language only and {@code isPublished = false}; an
     * admin later adds the missing active-language translations via {@link #updateRecipe} (one
     * locale per call) and only then calls this method.
     *
     * @param publicId public identifier of the recipe to publish
     * @param locale   BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return admin preview of the recipe, now published
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    RecipePreviewAdminDto publishRecipe(UUID publicId, @NonNull String locale);

    /**
     * Unpublishes a recipe (sets {@code isPublished = false}), hiding it from public
     * search/details/home feed again. Admin-only, same as {@link #publishRecipe} — no
     * author-or-admin fallback.
     *
     * @param publicId public identifier of the recipe to unpublish
     * @param locale   BCP-47 locale code used to select the translation for the preview; never {@code null}
     * @return admin preview of the recipe, now unpublished
     * @throws com.sb.sfrigola_core.domains.recipes.exception.NoRecipeFoundException
     *         if no recipe exists with the given public ID
     * @throws LocaleNotActiveException
     *         if {@code locale} does not match an active language (thrown by the languages domain bridge)
     */
    RecipePreviewAdminDto unpublishRecipe(UUID publicId, @NonNull String locale);

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
