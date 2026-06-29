package com.sb.sfrigola_core.domains.tags.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class TagStatusConverter implements AttributeConverter<TagStatus, String> {

    @Override
    public String convertToDatabaseColumn(TagStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TagStatus convertToEntityAttribute(String dbData) {
        return Arrays.stream(TagStatus.values()).filter(tagStatus -> tagStatus.getValue().equals(dbData)).findFirst().orElseThrow(
                () -> new IllegalArgumentException("Unknown database value: " + dbData)
        );
    }
}
