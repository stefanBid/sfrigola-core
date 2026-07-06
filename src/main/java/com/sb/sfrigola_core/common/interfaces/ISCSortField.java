package com.sb.sfrigola_core.common.interfaces;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Interface for typed sort-field enums used in {@link com.sb.sfrigola_core.common.models.contracts.SCFilterQuery}.
 * Each domain declares its own enum implementing this interface to expose only the columns it
 * actually allows sorting by, instead of passing raw hardcoded field-name strings around.
 */
public interface ISCSortField {

    String getEntityFieldName();

    /**
     * Resolves the enum constant of {@code enumClass} whose {@link #getEntityFieldName()} matches
     * {@code value} (case-insensitive).
     *
     * @param enumClass concrete sort-field enum to resolve against
     * @param value     raw value received from the client (e.g. a request param)
     * @return the matching enum constant
     * @throws IllegalArgumentException if no constant matches; message lists accepted values
     */
    static <E extends Enum<E> & ISCSortField> E fromString(Class<E> enumClass, String value) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(field -> field.getEntityFieldName().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid sort field: '" + value + "'. Accepted values (case insensitive): " +
                                Arrays.stream(enumClass.getEnumConstants())
                                        .map(ISCSortField::getEntityFieldName)
                                        .collect(Collectors.joining(", "))
                ));
    }
}
