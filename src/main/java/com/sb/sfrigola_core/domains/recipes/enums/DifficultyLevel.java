package com.sb.sfrigola_core.domains.recipes.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DifficultyLevel {

    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    @JsonValue
    private final String value;

    DifficultyLevel(String value) { this.value = value; }

    @JsonCreator
    public static DifficultyLevel fromValue(String value) {
        return Arrays.stream(values())
                .filter(level -> level.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid difficulty level: " + value + " Accepted values: " + Arrays.toString(values())));
    }
}
