package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class for pagination conversions used across service implementations.
 * Provides stateless helpers to map between query parameters and Spring Data types.
 */
public class SCPaginationUtils {

    private SCPaginationUtils() { throw new AssertionError("Utility class should not be instantiated"); }

    /**
     * Converts a {@link SCFilterQuery} into a Spring Data {@link Pageable}.
     *
     * @param filterQuery pagination and sorting parameters
     * @return {@link Pageable} ready to pass to repository methods
     */
    public static Pageable toPageable(SCFilterQuery<?> filterQuery) {
        if(filterQuery.sortBy() != null && filterQuery.sort() != null) {
            Sort.Direction direction = filterQuery.sort().isAsc() ? Sort.Direction.ASC : Sort.Direction.DESC;
            Sort sort = Sort.by(direction, filterQuery.sortBy());
            return PageRequest.of(filterQuery.page(), filterQuery.take(), sort);
        }
        return PageRequest.of(filterQuery.page(), filterQuery.take());
    }

    /**
     * Converts a Spring Data {@link Page} into a {@link SCPagedOptionDto}.
     *
     * @param page the page result returned by a repository method
     * @return {@link SCPagedOptionDto} containing pagination metadata
     */
    public static SCPagedOptionDto toPagedOption(Page<?> page) {
        return SCPagedOptionDto.of(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
