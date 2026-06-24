package com.sb.sfrigola_core.domains.users.controller;

import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;

import java.util.List;

/**
 * Controller-facing contract for category operations.
 * Locale is supplied by the caller via request parameter.
 * All methods succeed or throw a subclass of {@link com.sb.sfrigola_core.common.exception.ex.SCGeneralException}.
 */
public interface ICategoryService {

    /**
     * Returns all active categories with name and description localized to the requested locale.
     * If no translation exists for the requested locale, name and description are {@code null}.
     *
     * @param locale BCP-47 language code (e.g. "en", "it")
     * @return list of {@link CategoryDto} ordered by sort_order ascending
     */
    List<CategoryDto> getAll(String locale);
}
