package com.sb.sfrigola_core.domains.languages.enums;

import com.sb.sfrigola_core.common.interfaces.ISCSortField;
import lombok.Getter;

@Getter
public enum LanguageSortField implements ISCSortField {
    NAME("name"),
    CODE("code");

    private final String entityFieldName;

    LanguageSortField(String entityFieldName) { this.entityFieldName = entityFieldName; }

    public static LanguageSortField fromString(String value) {
        return ISCSortField.fromString(LanguageSortField.class, value);
    }
}
