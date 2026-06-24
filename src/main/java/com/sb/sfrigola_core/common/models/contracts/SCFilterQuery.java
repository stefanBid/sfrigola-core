package com.sb.sfrigola_core.common.models.contracts;

import com.sb.sfrigola_core.common.enums.SortDirection;

public record SCFilterQuery<T>(
    // Searching
    String searchKey,
    // Sorting
    String sortBy,
    SortDirection sort,
    // Paging
    int take,
    int page,
    // Complex Filtering
    T other
)  {

    public static  SCFilterQuery<Void> essential(String sortBy, SortDirection sort, int take, int page) {
        return new SCFilterQuery<>(null,sortBy, sort, take, page, null);
    }

    public static  SCFilterQuery<Void> essentialWithSearch(String searchKey, String sortBy, SortDirection sort, int take, int page) {
        return new SCFilterQuery<>(searchKey,sortBy, sort, take, page, null);
    }

    public static <T> SCFilterQuery<T> powerful(String searchKey, String sortBy, SortDirection sort, int take, int page, T other) {
        return new SCFilterQuery<>(searchKey,sortBy, sort, take, page, other);
    }
}
