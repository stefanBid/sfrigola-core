package com.sb.sfrigola_core.domains.recipes.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DifficultyLevelConverter implements AttributeConverter<DifficultyLevel, String> {

    @Override
    public String convertToDatabaseColumn(DifficultyLevel attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public DifficultyLevel convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DifficultyLevel.fromValue(dbData);
    }
}
