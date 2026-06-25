package com.sb.sfrigola_core.domains.categories.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDetailsAdminDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;
import com.sb.sfrigola_core.domains.categories.dto.CategoryPreviewAdminDto;

/**
 * Controller-facing contract for the categories domain.
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
     *         {@link com.sb.sfrigola_core.domains.categories.dto.CategoryPreviewAdminDto}; never {@code null}
     */
    SCPagedResult<CategoryPreviewAdminDto> getAllAdmin(SCFilterQuery<Void> filterQuery, String locale, Boolean isActive);

    /**
     * Returns the full admin detail of a single category, including all existing translations
     * and the list of active languages that still lack a translation.
     *
     * @param publicId the UUID string identifying the category
     * @param locale   BCP-47 language code used for the name/description preview field
     * @return a {@link com.sb.sfrigola_core.domains.categories.dto.CategoryDetailsAdminDto}
     * @throws com.sb.sfrigola_core.domains.categories.exception.NoCategoryFoundException
     *         if no category exists with the given {@code publicId}
     */
    CategoryDetailsAdminDto getByPublicIdAdmin(String publicId, String locale);

}
