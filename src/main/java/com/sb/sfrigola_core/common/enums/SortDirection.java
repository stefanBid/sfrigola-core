package com.sb.sfrigola_core.common.enums;

public enum SortDirection {
    ASC, DESC;

    public boolean isAsc() {
        return this == ASC;
    }
    public boolean isDesc() {
        return this == DESC;
    }

    public static SortDirection fromString(String  direction) {
        try {
            return SortDirection.valueOf(direction.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid sort direction: " + direction + "It Has to be either 'asc' or 'desc' (case insensitive)", e);
        }
    }


}

