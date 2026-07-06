package com.sb.sfrigola_core.common.models.contracts;

import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.common.interfaces.ISCSortField;

public record SCFilterQuery<T>(
    // Searching
    String searchKey,
    // Sorting
    ISCSortField sortBy,
    SortDirection sort,
    // Paging
    int take,
    int page,
    // Complex Filtering
    T other
)  {

    public static SCFilterQuery<Void> essential(ISCSortField sortBy, SortDirection sort, int take, int page) {
        return new SCFilterQuery<>(null,sortBy, sort, take, page, null);
    }

    public static SCFilterQuery<Void> essentialWithSearch(String searchKey, ISCSortField sortBy, SortDirection sort, int take, int page) {
        return new SCFilterQuery<>(searchKey,sortBy, sort, take, page, null);
    }

    public static SCFilterQuery<Void> pagedOnly(int take, int page) {
        return new SCFilterQuery<>(null, null, null, take, page, null);
    }

    public static SCFilterQuery<Void> pageWithSearch(String searchKey, int take, int page) {
        return new SCFilterQuery<>(searchKey, null, null, take, page, null);
    }

    public static <T> SCFilterQuery<T> powerful(String searchKey, ISCSortField sortBy, SortDirection sort, int take, int page, T other) {
        return new SCFilterQuery<>(searchKey,sortBy, sort, take, page, other);
    }
}
