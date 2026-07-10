package com.sb.sfrigola_core.domains.tags.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sb.sfrigola_core.common.exception.ex.SCEnumValidationException;
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
                .orElseThrow(() -> new SCEnumValidationException(
                        TagScope.class.getSimpleName(), value, values(), TagScope::getValue));
    }
}
