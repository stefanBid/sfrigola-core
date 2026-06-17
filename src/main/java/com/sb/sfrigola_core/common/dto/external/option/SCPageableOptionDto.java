package com.sb.sfrigola_core.common.dto.external.option;

import java.io.Serializable;

public record SCPageableOptionDto(
    Integer currentPage,      // Current page number (1-based)
    Integer pageSize,         // Number of items per page
    Long totalElements,       // Total number of elements across all pages
    Integer totalPages,       // Total number of pages
    Boolean hasMore           // Whether there are more pages available
) implements Serializable {
    public static SCPageableOptionDto of(Integer currentPage, Integer pageSize, Long totalElements, Integer totalPages, Boolean hasMore) {
        return new SCPageableOptionDto(currentPage, pageSize, totalElements, totalPages, hasMore);
    }
}
