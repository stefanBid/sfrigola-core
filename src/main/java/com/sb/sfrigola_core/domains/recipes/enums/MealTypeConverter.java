package com.sb.sfrigola_core.domains.recipes.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MealTypeConverter implements AttributeConverter<MealType, String> {

    @Override
    public String convertToDatabaseColumn(MealType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public MealType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MealType.fromValue(dbData);
    }
}
