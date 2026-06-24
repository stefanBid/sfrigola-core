package com.sb.sfrigola_core.common.models.contracts;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;

import java.util.List;

public record SCPagedResult<T>(
    List<T> content,
    SCPagedOptionDto pagedOptionDto
) {
}
