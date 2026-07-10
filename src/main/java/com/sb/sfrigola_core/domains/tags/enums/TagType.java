package com.sb.sfrigola_core.domains.tags.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sb.sfrigola_core.common.exception.ex.SCEnumValidationException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TagType {

    RECIPE("recipe"),
    FLAVOR("flavor"),
    TEXTURE("texture"),
    SEASON("season"),
    DIETARY("dietary");

    @JsonValue
    private final String value;

    TagType(String value) { this.value = value; }

    @JsonCreator
    public static TagType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new SCEnumValidationException(
                        TagType.class.getSimpleName(), value, values(), TagType::getValue));
    }
}
