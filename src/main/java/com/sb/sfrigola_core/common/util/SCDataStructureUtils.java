package com.sb.sfrigola_core.common.util;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SCDataStructureUtils {

    private SCDataStructureUtils() {
        throw new AssertionError("Cannot instantiate SCDataStructureUtils");
    }


    /**
     * Guards against a {@code null} optional list field in a request DTO (e.g. {@code recipeTagsIds},
     * {@code ingredients}) so downstream stream/loop code never needs its own null check.
     *
     * @param list the list to guard, possibly {@code null}
     * @return {@code list} unchanged, or an empty list if it was {@code null}
     */
    public static <E> List<E> nullSafeList(List<E> list) {
        return list != null ? list : List.of();
    }

    /**
     * Convenience for {@link #joinListIntoString(List, String, boolean)} with
     * {@code includeLastItem = false}: the delimiter is inserted only between elements,
     * never appended after the last one.
     *
     * @param list      the list to join, possibly {@code null} (treated as empty)
     * @param delimiter separator inserted between elements; never {@code null}
     * @return the joined string, empty if {@code list} is {@code null} or empty
     */
    public static String joinListIntoString(List<String> list, @NonNull String delimiter) {
        return joinListIntoString(list, delimiter, false);
    }

    /**
     * Joins {@code list} into a single string, guarding against a {@code null} list via
     * {@link #nullSafeList}.
     *
     * @param list            the list to join, possibly {@code null} (treated as empty)
     * @param delimiter       separator inserted between elements; never {@code null}
     * @param includeLastItem if {@code true}, {@code delimiter} is also appended after the last
     *                        element (e.g. so each element can be wrapped uniformly, like a
     *                        trailing period on every sentence); if {@code false}, it is inserted
     *                        only between elements
     * @return the joined string, empty if {@code list} is {@code null} or empty
     */
    public static String joinListIntoString(List<String> list, @NonNull String delimiter, boolean includeLastItem) {
        List<String> safeList = nullSafeList(list);
        if (safeList.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < safeList.size(); i++) {
            sb.append(safeList.get(i));
            if (i < safeList.size() - 1 || includeLastItem) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Splits {@code value} back into a list, the inverse of {@link #joinListIntoString}.
     *
     * @param value     the string to split, possibly {@code null} (treated as empty)
     * @param delimiter separator to split on; never {@code null}
     * @return the split parts, empty if {@code value} is {@code null} or blank
     */
    public static List<String> splitStringIntoList(String value, @NonNull String delimiter) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split(Pattern.quote(delimiter)));
    }
}
