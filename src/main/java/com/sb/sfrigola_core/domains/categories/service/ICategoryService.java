package com.sb.sfrigola_core.domains.categories.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.categories.dto.admin.CategoryDetailsAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.admin.CategoryPreviewAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.admin.CategoryInputDto;
import jakarta.annotation.Nullable;

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
     *         {@link com.sb.sfrigola_core.domains.categories.dto.CategoryDto}; never {@code null}
     */
    SCPagedResult<CategoryDto> getAll(SCFilterQuery<Void> filterQuery, String locale);

    /**
     * Returns a paginated admin preview of categories, including localization coverage counts
     * (present and missing) to support CMS overviews.
     *
     * @param filterQuery pagination, sorting, and optional search key
     * @param locale      BCP-47 language code used for the name/description preview field
     * @param isActive    when non-{@code null}, filters by active/inactive status
     * @return a {@link com.sb.sfrigola_core.common.models.contracts.SCPagedResult} of
     *         {@link CategoryPreviewAdminDto}; never {@code null}
     */
    SCPagedResult<CategoryPreviewAdminDto> getAllAdmin(SCFilterQuery<Void> filterQuery, String locale, Boolean isActive);

    /**
     * Returns the full admin detail of a single category, including all existing translations
     * and the list of active languages that still lack a translation.
     *
     * @param publicId the UUID string identifying the category
     * @return a {@link CategoryDetailsAdminDto}
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given {@code publicId}
     */
    CategoryDetailsAdminDto getByPublicIdAdmin(UUID publicId);


    /**
     * Creates a new category and appends it at the end of its group by {@code sort_order}.
     * Root categories ({@code parentPublicId} is {@code null}) are appended after existing root categories.
     * Child categories are appended after the existing siblings of the given parent.
     * <p>
     * Translations provided in {@code dto} are inserted as-is; no merge occurs on create.
     *
     * @param dto            creation payload — slug, isActive, and at least one translation
     * @param parentPublicId UUID of the parent category; {@code null} creates a root category
     * @return admin preview of the newly created category
     * @throws com.sb.sfrigola_core.domains.categories.exception.CategorySlugAlreadyExistsException
     *         if a category with the same slug already exists
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if {@code parentPublicId} is non-{@code null} but no matching category exists
     * @throws com.sb.sfrigola_core.domains.categories.exception.InvalidCategoryLocaleException
     *         if any translation references a locale that is not active
     * @throws com.sb.sfrigola_core.domains.categories.exception.DuplicateCategoryLocaleException
     *         if the same locale appears more than once in the translations list
     */
    CategoryPreviewAdminDto createNewCategory(CategoryInputDto dto, @Nullable UUID parentPublicId);

    /**
     * Updates an existing category's slug, active status, and translations.
     * <p>
     * Translations are merged: locales present in {@code dto} are added if missing or updated if existing;
     * locales not included in {@code dto} are left untouched.
     * Parent and {@code sort_order} are never modified by this operation —
     * tree restructuring is handled exclusively by the dedicated reorder endpoint.
     *
     * @param dto      update payload — slug, isActive, and translations to upsert
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
    CategoryPreviewAdminDto updateCategory(CategoryInputDto dto, UUID publicId);

    /**
     * Deletes a category and all its translations.
     * <p>
     * Deletion is blocked if the category has children; callers must reassign or delete
     * child categories first. Translations are removed via JPA cascade ({@code CascadeType.ALL}).
     *
     * @param publicId UUID identifying the category to delete
     * @return admin preview of the deleted category (last known state)
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given {@code publicId}
     * @throws com.sb.sfrigola_core.domains.categories.exception.CategoryHasChildrenException
     *         if the category has one or more child categories
     */
    CategoryPreviewAdminDto deleteCategory(UUID publicId);

}
