package com.sb.sfrigola_core.domains.ingredients.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class IngredientFoodGroupConverter implements AttributeConverter<IngredientFoodGroup, String> {

    @Override
    public String convertToDatabaseColumn(IngredientFoodGroup attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public IngredientFoodGroup convertToEntityAttribute(String dbData) {
        return dbData == null ? null : IngredientFoodGroup.fromString(dbData);
    }
}
