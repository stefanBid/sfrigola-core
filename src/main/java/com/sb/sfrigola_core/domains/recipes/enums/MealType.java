package com.sb.sfrigola_core.domains.recipes.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum MealType {

    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    SNACK("snack"),
    DESSERT("dessert"),
    APPETIZER("appetizer");

    @JsonValue
    private final String value;

    MealType(String value) { this.value = value; }

    @JsonCreator
    public static MealType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid meal type: " + value + " Accepted values: " + Arrays.toString(values())));
    }
}
