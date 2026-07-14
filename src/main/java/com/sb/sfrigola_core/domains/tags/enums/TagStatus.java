package com.sb.sfrigola_core.domains.tags.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.sb.sfrigola_core.common.exception.ex.SCEnumValidationException;
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
                .orElseThrow(() -> new SCEnumValidationException(
                        TagStatus.class.getSimpleName(), value, values(), TagStatus::getValue));
    }
}
