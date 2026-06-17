package com.sb.sfrigola_core.common.dto.internal;

import com.sb.sfrigola_core.common.enums.SortDirection;

import java.io.Serializable;

public record SCFilterParamsServiceArgs(
    String sortBy,
    SortDirection sort,
    String take,
    String page
)  {
}
