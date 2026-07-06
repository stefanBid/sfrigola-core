package com.sb.sfrigola_core.common.enums;


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
        return Arrays.stream(values()).filter(dir -> dir.name().equalsIgnoreCase(direction)).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid sort direction: " + direction + "It Has to be either 'asc' or 'desc' (case insensitive)"));
    }

}

