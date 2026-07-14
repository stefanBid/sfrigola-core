package com.sb.sfrigola_core.common.exception.ex;

import com.sb.sfrigola_core.common.enums.GeneralErrorCode;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class SCEnumValidationException extends SCGeneralException {

    public SCEnumValidationException(String enumName, String fromStringValue, List<String> validEnumValues) {
        super(
                HttpStatus.BAD_REQUEST,
                GeneralErrorCode.INVALID_ENUM_CODE,
                String.format("Invalid value '%s' for enum '%s'. Valid values are (ignore-case): %s", fromStringValue, enumName, validEnumValues)
        );
    }

    /**
     * Convenience for name-based enums: pass {@code values()} directly, matched against
     * {@link Enum#name()} — no stream/map needed at the call site.
     */
    public SCEnumValidationException(String enumName, String fromStringValue, Enum<?>[] values) {
        this(enumName, fromStringValue, Arrays.stream(values).map(Enum::name).toList());
    }

    /**
     * Convenience for value-based enums: pass {@code values()} plus the accessor for the
     * client-facing value (e.g. {@code DifficultyLevel::getValue}) — no stream/map needed
     * at the call site.
     */
    public <E> SCEnumValidationException(String enumName, String fromStringValue, E[] values, Function<E, String> valueExtractor) {
        this(enumName, fromStringValue, Arrays.stream(values).map(valueExtractor).toList());
    }
}
