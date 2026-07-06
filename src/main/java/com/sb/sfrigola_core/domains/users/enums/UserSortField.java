package com.sb.sfrigola_core.domains.users.enums;

import com.sb.sfrigola_core.common.interfaces.ISCSortField;
import lombok.Getter;

@Getter
public enum UserSortField implements ISCSortField {
    USERNAME("username"),
    EMAIL("email"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName");

    private final String entityFieldName;

    UserSortField(String entityFieldName) { this.entityFieldName = entityFieldName; }

    public static UserSortField fromString(String value) {
        return ISCSortField.fromString(UserSortField.class, value);
    }
}
