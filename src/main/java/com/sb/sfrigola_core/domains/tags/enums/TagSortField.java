package com.sb.sfrigola_core.domains.tags.enums;

import com.sb.sfrigola_core.common.interfaces.ISCSortField;
import lombok.Getter;

@Getter
public enum TagSortField implements ISCSortField {
    LABEL("label"),
    TYPE("type"),
    SCOPE("scope"),
    STATUS("status");

    private final String entityFieldName;

    TagSortField(String entityFieldName) { this.entityFieldName = entityFieldName; }

    public static TagSortField fromString(String value) {
        return ISCSortField.fromString(TagSortField.class, value);
    }
}
