package com.sb.sfrigola_core.domains.tags.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TagScope {

    RECIPE("recipe"),
    INGREDIENT("ingredient"),
    BOTH("both");

    @JsonValue
    private final String value;

    TagScope(String value) { this.value = value; }

    @JsonCreator
    public static TagScope fromValue(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid tag scope: " + value + " Accepted values: " + Arrays.toString(values())));
    }
}
