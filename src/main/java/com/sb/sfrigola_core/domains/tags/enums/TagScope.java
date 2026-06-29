package com.sb.sfrigola_core.domains.tags.enums;

import lombok.Getter;

@Getter
public enum TagScope {

    RECIPE("recipe"),
    INGREDIENT("ingredient"),
    BOTH("both");

    private final String value;

    TagScope(String value) { this.value = value; }
}
