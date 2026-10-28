package com.vetclinic.common;

import java.util.Locale;
import java.util.stream.Collectors;

/** Helpers shared by the API enums so they accept {@code "dog"} as well as {@code "DOG"} on input. */
public final class EnumSupport {

    private EnumSupport() {
    }

    /**
     * Case and separator insensitive enum lookup. Throws {@link IllegalArgumentException} with the list of
     * accepted values, which Spring turns into a readable 400 response.
     */
    public static <E extends Enum<E>> E parse(Class<E> type, String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(normalized)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("'%s' is not a valid %s. Accepted values: %s"
                .formatted(rawValue, type.getSimpleName(), allowed(type)));
    }

    private static <E extends Enum<E>> String allowed(Class<E> type) {
        return java.util.Arrays.stream(type.getEnumConstants())
                .map(Enum::name)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
    }
}
