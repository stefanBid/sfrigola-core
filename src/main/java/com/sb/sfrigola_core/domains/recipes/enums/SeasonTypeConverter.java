package com.sb.sfrigola_core.domains.recipes.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SeasonTypeConverter implements AttributeConverter<SeasonType, String> {

    @Override
    public String convertToDatabaseColumn(SeasonType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public SeasonType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SeasonType.fromValue(dbData);
    }
}
