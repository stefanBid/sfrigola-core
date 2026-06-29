package com.sb.sfrigola_core.domains.tags.enums;

import lombok.Getter;

@Getter
public enum TagType {

    RECIPE("recipe"),
    FLAVOR("flavor"),
    TEXTURE("texture"),
    SEASON("season"),
    DIETARY("dietary");

    private final String value;

    TagType(String value) { this.value = value; }
}
