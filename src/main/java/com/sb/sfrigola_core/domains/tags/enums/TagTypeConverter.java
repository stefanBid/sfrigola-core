package com.sb.sfrigola_core.domains.tags.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class TagTypeConverter implements AttributeConverter<TagType, String> {

    @Override
    public String convertToDatabaseColumn(TagType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TagType convertToEntityAttribute(String dbData) {
        return Arrays.stream(TagType.values()).filter(tagType -> tagType.getValue().equals(dbData)).findFirst().orElseThrow(
                () -> new IllegalArgumentException("Unknown database value: " + dbData)
        );
    }
}
