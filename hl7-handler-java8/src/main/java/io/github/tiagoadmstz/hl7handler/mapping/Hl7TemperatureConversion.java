package io.github.tiagoadmstz.hl7handler.mapping;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Converts temperatures between Celsius and Fahrenheit for values whose unit is coded in an HL7 unit field.
 * A unit is recognised by any of its components (MDC code, MDC name, UCUM code or symbol), so the rule works
 * for each notation the devices use.
 */
final class Hl7TemperatureConversion {

    private static final String[] CELSIUS_TOKENS = {"268192", "MDC_DIM_DEGC", "Cel", "°C"};
    private static final String[] FAHRENHEIT_TOKENS = {"266560", "MDC_DIM_FAHR", "[degF]", "°F"};

    private Hl7TemperatureConversion() {}

    /**
     * Detects the temperature unit of a unit field, or {@code null} when no component is a temperature unit.
     */
    static Hl7TemperatureUnit unitOf(String unitField, char componentSeparator) {
        if (unitField == null) {
            return null;
        }
        for (String component : unitField.split(Pattern.quote(String.valueOf(componentSeparator)), -1)) {
            String token = component.trim();
            if (indexOf(CELSIUS_TOKENS, token) >= 0) {
                return Hl7TemperatureUnit.CELSIUS;
            }
            if (indexOf(FAHRENHEIT_TOKENS, token) >= 0) {
                return Hl7TemperatureUnit.FAHRENHEIT;
            }
        }
        return null;
    }

    /**
     * Converts a numeric text from one unit to the other. The result is rounded HALF_UP to the number of decimals
     * of the input, with at least one. Returns {@code null} when the text is not a decimal number.
     */
    static Double convert(String rawValue, Hl7TemperatureUnit from, Hl7TemperatureUnit to) {
        BigDecimal value;
        try {
            value = new BigDecimal(rawValue.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
        int places = Math.max(1, value.scale());
        BigDecimal converted;
        if (from == Hl7TemperatureUnit.CELSIUS) {
            converted = value.multiply(BigDecimal.valueOf(9)).divide(BigDecimal.valueOf(5), MathContext.DECIMAL128).add(BigDecimal.valueOf(32));
        } else {
            converted = value.subtract(BigDecimal.valueOf(32)).multiply(BigDecimal.valueOf(5)).divide(BigDecimal.valueOf(9), MathContext.DECIMAL128);
        }
        return converted.setScale(places, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Replaces the components of the unit field that name the other temperature unit by the target unit,
     * in the same notation. Returns the field unchanged when it names no temperature unit of the other kind.
     */
    static String adjustUnit(String unitField, char componentSeparator, Hl7TemperatureUnit target) {
        if (unitField == null) {
            return null;
        }
        String[] otherTokens = target == Hl7TemperatureUnit.CELSIUS ? FAHRENHEIT_TOKENS : CELSIUS_TOKENS;
        String[] targetTokens = target == Hl7TemperatureUnit.CELSIUS ? CELSIUS_TOKENS : FAHRENHEIT_TOKENS;
        String separator = String.valueOf(componentSeparator);
        String[] components = unitField.split(Pattern.quote(separator), -1);
        for (int i = 0; i < components.length; i++) {
            int index = indexOf(otherTokens, components[i].trim());
            if (index >= 0) {
                components[i] = targetTokens[index];
            }
        }
        return String.join(separator, components);
    }

    private static int indexOf(String[] tokens, String token) {
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].equalsIgnoreCase(token)) {
                return i;
            }
        }
        return -1;
    }
}
