package com.sb.sfrigola_core.domains.tags.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;

@Converter(autoApply = true)
public class TagScopeConverter implements AttributeConverter<TagScope, String> {

    @Override
    public String convertToDatabaseColumn(TagScope attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TagScope convertToEntityAttribute(String dbData) {
        return Arrays.stream(TagScope.values()).filter(tagScope -> tagScope.getValue().equals(dbData)).findFirst().orElseThrow(
                () -> new IllegalArgumentException("Unknown database value: " + dbData)
        );
    }
}
