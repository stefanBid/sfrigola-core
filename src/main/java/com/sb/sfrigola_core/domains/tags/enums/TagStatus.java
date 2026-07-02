package com.sb.sfrigola_core.domains.tags.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TagStatus {

    APPROVED("approved"),
    PENDING("pending"),
    REJECTED("rejected");

    @JsonValue
    private final String value;

    TagStatus(String value) { this.value = value; }

    @JsonCreator
    public static TagStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid tag status: " + value + " Accepted values: " + Arrays.toString(values())));
    }
}
