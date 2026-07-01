package com.sb.sfrigola_core.domains.tags.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TagStatusConverter implements AttributeConverter<TagStatus, String> {

    @Override
    public String convertToDatabaseColumn(TagStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TagStatus convertToEntityAttribute(String dbData) {
        return TagStatus.fromValue(dbData);
    }
}
