package com.sb.sfrigola_core.common.enums;


import com.sb.sfrigola_core.common.exception.ex.SCEnumValidationException;

import java.util.Arrays;

public enum SortDirection {
    ASC, DESC;

    public boolean isAsc() {
        return this == ASC;
    }
    public boolean isDesc() {
        return this == DESC;
    }

    public static SortDirection fromString(String  direction) {
        return Arrays.stream(values()).filter(dir -> dir.name().equalsIgnoreCase(direction)).findFirst()
                .orElseThrow(() -> new SCEnumValidationException(SortDirection.class.getSimpleName(), direction, values()));
    }

}

