package com.sb.sfrigola_core.common.dto.internal;

import com.sb.sfrigola_core.common.enums.SortDirection;

public record SCFilterParamsServiceArgs(
    String sortBy,
    SortDirection sort,
    int take,
    int page
)  {
}
