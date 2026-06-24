package com.sb.sfrigola_core.common.dto.option;

import java.io.Serializable;

public record SCPagedOptionDto(
    Integer currentPage,      // Current page number (1-based)
    Integer pageSize,         // Number of items per page
    Long totalElements,       // Total number of elements across all pages
    Integer totalPages,       // Total number of pages
    Boolean hasMore           // Whether there are more pages available
) implements Serializable {
    public static SCPagedOptionDto of(Integer currentPage, Integer pageSize, Long totalElements, Integer totalPages, Boolean hasMore) {
        return new SCPagedOptionDto(currentPage, pageSize, totalElements, totalPages, hasMore);
    }

    public static SCPagedOptionDto noItems(){
        return new SCPagedOptionDto(0, 0, 0L, 0, false);
    }
}
