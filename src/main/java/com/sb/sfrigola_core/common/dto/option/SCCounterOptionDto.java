package com.sb.sfrigola_core.common.dto.option;

import java.io.Serializable;

public record SCCounterOptionDto(
    Long count
) implements Serializable {
    public static SCCounterOptionDto of(Long count) {
        return new SCCounterOptionDto(count);
    }

    public static final SCCounterOptionDto SINGLE_ELEMENT = new SCCounterOptionDto(1L);
}

