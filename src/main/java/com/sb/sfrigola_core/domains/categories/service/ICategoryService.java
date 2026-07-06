package com.sb.sfrigola_core.domains.categories.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.categories.dto.input.UpdateCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryDetailsAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryPublicViewDto;
import com.sb.sfrigola_core.domains.categories.dto.view.CategoryPreviewAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.input.AddCategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.input.ReorderedCategoriesTreeDto;
import jakarta.annotation.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;

/**
 * Controller-facing contract for the categories' domain.
 * Reads the security context internally where needed.
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 */
public interface ICategoryService {

    /**
     * Returns a paginated list of active categories localized for the requested locale.
     * Only categories that have a translation for {@code locale} are included.
     *
     * @param filterQuery pagination and sorting parameters
     * @param locale      BCP-47 language code used to filter and localize results
     * @return a {@link com.sb.sfrigola_core.common.models.contracts.SCPagedResult} of
     *         {@link CategoryPublicViewDto}; never {@code null}
     * @throws com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException
     *         if {@code locale} does not match an active language
     */
    SCPagedResult<CategoryPublicViewDto> getAll(SCFilterQuery<Void> filterQuery, String locale);

    /**
     * Returns a paginated admin preview of ALL categories (regardless of translation coverage for
     * {@code locale}), including localization coverage counts (present and missing) to support CMS overviews.
     * <p>
     * {@code locale} is mandatory — never filters out categories: it only selects which translation
     * is used for the preview. If no translation exists for {@code locale}, the preview fields are {@code null}.
     *
     * @param filterQuery pagination, sorting, and optional search key
     * @param locale      BCP-47 language code used for the name/description preview field; never {@code null}
     * @param isActive    when non-{@code null}, filters by active/inactive status
     * @return a {@link com.sb.sfrigola_core.common.models.contracts.SCPagedResult} of
     *         {@link CategoryPreviewAdminDto}; never {@code null}
     * @throws com.sb.sfrigola_core.domains.categories.exception.InvalidCategoryLocaleException
     *         if {@code locale} does not match an active language
     */
    SCPagedResult<CategoryPreviewAdminDto> getAllAdmin(SCFilterQuery<Void> filterQuery, @NonNull String locale, Boolean isActive);

    /**
     * Returns the admin detail of a single category, localized for the requested locale.
     * <p>
     * {@code locale} is mandatory and never filters — it only selects which translation is
     * returned. If no translation exists for {@code locale}, {@code specificTranslation}'s
     * fields are {@code null}. Coverage across all languages is exposed by {@link #getAllAdmin}
     * ({@code translatedLanguages}), not here.
     *
     * @param publicId the UUID string identifying the category
     * @param locale   BCP-47 language code used for the translation fields; never {@code null}
     * @return a {@link CategoryDetailsAdminDto}
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given {@code publicId}
     * @throws com.sb.sfrigola_core.domains.languages.exception.LocaleNotActiveException
     *         if {@code locale} does not match an active language
     */
    CategoryDetailsAdminDto getByPublicIdAdmin(UUID publicId, @NonNull String locale);


    /**
     * Creates a new category and appends it at the end of its group by {@code sort_order}.
     * Root categories ({@code parentPublicId} is {@code null}) are appended after existing root categories.
     * Child categories are appended after the existing siblings of the given parent.
     * <p>
     * Translations provided in {@code dto} are inserted as-is; no merge occurs on create.
     *
     * @param addCategoryDto creation payload — slug, isActive, and at least one translation
     * @param parentPublicId UUID of the parent category; {@code null} creates a root category
     * @param locale BCP-47 language code used for the name/description preview field; never {@code null}
     * @return admin preview of the newly created category
     * @throws com.sb.sfrigola_core.domains.categories.exception.CategorySlugAlreadyExistsException
     *         if a category with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code parentPublicId} is non-{@code null} but no matching category exists
     * @throws com.sb.sfrigola_core.domains.categories.exception.InvalidCategoryLocaleException
     *         if any translation references a locale that is not active
     * @throws com.sb.sfrigola_core.domains.categories.exception.DuplicateCategoryLocaleException
     *         if the same locale appears more than once in the translations list
     * @throws com.sb.sfrigola_core.domains.categories.exception.MissingCategoryLocalesException
     *         if the translations list does not cover all active languages
     */
    CategoryPreviewAdminDto createNewCategory(AddCategoryDto addCategoryDto, @Nullable UUID parentPublicId, @NonNull String locale);

    /**
     * Updates an existing category's slug, active status, and translations.
     * <p>
     * Translations are merged: locales present in {@code updateCategoryDto} are added if missing or updated if existing;
     * locales not included in {@code updateCategoryDto} are left untouched.
     * Parent and {@code sort_order} are never modified by this operation —
     * tree restructuring is handled exclusively by the dedicated reorder endpoint.
     *
     * @param updateCategoryDto update payload — slug, isActive, and translations to upsert
     * @param publicId UUID identifying the category to update
     * @return admin preview of the updated category
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given {@code publicId}
     * @throws com.sb.sfrigola_core.domains.categories.exception.CategorySlugAlreadyExistsException
     *         if the new slug is already taken by a different category
     * @throws com.sb.sfrigola_core.domains.categories.exception.InvalidCategoryLocaleException
     *         if any translation references a locale that is not active
     * @throws com.sb.sfrigola_core.domains.categories.exception.DuplicateCategoryLocaleException
     *         if the same locale appears more than once in the translations list
     */
    CategoryPreviewAdminDto updateCategory(UpdateCategoryDto updateCategoryDto, UUID publicId);

    /**
     * Deletes a category and all its translations.
     * <p>
     * Deletion is blocked if the category has children; callers must reassign or delete
     * child categories first. Translations are removed via JPA cascade ({@code CascadeType.ALL}).
     * No preview DTO is returned — the category no longer exists, so only its identifier
     * is handed back (e.g. for the caller to remove it from a client-side list).
     *
     * @param publicId UUID identifying the category to delete
     * @return the {@code publicId} of the deleted category
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given {@code publicId}
     * @throws com.sb.sfrigola_core.domains.categories.exception.CategoryHasChildrenException
     *         if the category has one or more child categories
     */
    UUID deleteCategory(UUID publicId);


    /**
     * Reorders all categories within a single group atomically.
     * One call covers exactly one group: either all root categories ({@code parentPublicId} is {@code null})
     * or all direct children of the specified parent.
     * <p>
     * The caller must supply the complete ordered list of public IDs for the target group.
     * The array index becomes the new {@code sort_order} (0-based).
     * The set of IDs in {@code dto.orderedPublicIds} must match exactly the set of categories
     * in the target group — no additions, omissions, or duplicates are allowed.
     *
     * @param reorderDto reorder payload — optional parent UUID and the full ordered list of category public IDs
     * @return the reordered group as a flat list of admin previews, in the new sort order
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code dto.parentPublicId} is non-{@code null} but no matching category exists
     * @throws com.sb.sfrigola_core.domains.categories.exception.CategoryReorderMismatchException
     *         if {@code dto.orderedPublicIds} does not match exactly the categories in the target group
     */
    List<CategoryPreviewAdminDto> reorderCategories(ReorderedCategoriesTreeDto reorderDto);

}
