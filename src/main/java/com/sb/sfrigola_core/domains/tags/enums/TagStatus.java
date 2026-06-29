package com.sb.sfrigola_core.domains.tags.enums;

import lombok.Getter;

@Getter
public enum TagStatus {

    APPROVED("approved"),
    PENDING("pending"),
    REJECTED("rejected");

    private final String value;

    TagStatus(String value) { this.value = value; }
}
