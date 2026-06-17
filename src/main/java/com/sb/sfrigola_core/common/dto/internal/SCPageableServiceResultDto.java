package com.sb.sfrigola_core.common.dto.internal;

import com.sb.sfrigola_core.common.dto.external.option.SCPageableOptionDto;

import java.util.List;

public record SCPageableServiceResultDto<T>(
    List<T> content,
    SCPageableOptionDto pageableOption
) {
}
