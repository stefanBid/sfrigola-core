package com.sb.sfrigola_core.domains.ingredients.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum IngredientFoodGroup {

    VEGETABLE("vegetable"),
    FRUIT("fruit"),
    DAIRY("dairy"),
    PROTEIN("protein"),
    GRAIN("grain"),
    FAT("fat"),
    HERB("herb"),
    BEVERAGE("beverage"),
    OTHER("other");

    @JsonValue
    private final String value;

    IngredientFoodGroup(String value) { this.value = value; }

    @JsonCreator
    public static IngredientFoodGroup fromString(String value) {
        return Arrays.stream(values())
                .filter(category -> category.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid ingredient food group: " + value + " Accepted values: " + Arrays.toString(values())));
    }
}
