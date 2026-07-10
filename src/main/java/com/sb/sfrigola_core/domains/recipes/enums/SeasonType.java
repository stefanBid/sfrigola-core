package com.sb.sfrigola_core.domains.recipes.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sb.sfrigola_core.common.exception.ex.SCEnumValidationException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SeasonType {

    SPRING("spring"),
    SUMMER("summer"),
    AUTUMN("autumn"),
    WINTER("winter"),
    ALL_YEAR("all_year");

    @JsonValue
    private final String value;

    SeasonType(String value) { this.value = value; }

    @JsonCreator
    public static SeasonType fromValue(String value) {
        return Arrays.stream(values())
                .filter(season -> season.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new SCEnumValidationException(
                        SeasonType.class.getSimpleName(), value, values(), SeasonType::getValue));
    }
}
