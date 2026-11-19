package io.github.tiagoadmstz.hl7handler.mapping;

import java.util.Locale;

/**
 * Temperature unit that a mapping converts numeric values to. Set on the builder or as the
 * {@code temperatura} metadata of the YAML mapping.
 */
public enum Hl7TemperatureUnit {

    CELSIUS,
    FAHRENHEIT;

    /**
     * Resolves a unit from its name: {@code celsius}, {@code c} or {@code cel} for Celsius, and
     * {@code fahrenheit} or {@code f} for Fahrenheit, case-insensitive.
     *
     * @param name the unit name; may be {@code null}
     * @return the unit, or {@code null} when the name is unknown or blank
     */
    public static Hl7TemperatureUnit fromName(String name) {
        if (name == null) {
            return null;
        }
        String clean = name.trim().toLowerCase(Locale.ROOT);
        if ("celsius".equals(clean) || "c".equals(clean) || "cel".equals(clean)) {
            return CELSIUS;
        }
        if ("fahrenheit".equals(clean) || "f".equals(clean)) {
            return FAHRENHEIT;
        }
        return null;
    }
}
