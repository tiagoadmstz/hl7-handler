package io.github.tiagoadmstz.hl7handler.mapping;

import io.github.tiagoadmstz.hl7handler.model.Hl7DateTime;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Functional interface and registry for type conversion in HL7 mappings.
 * Supports built-in converters (texto, numero, data, booleano) and custom converters.
 */
@FunctionalInterface
public interface Hl7TypeConverter {

    Object convert(String rawValue);

    String TEXTO = "texto";
    String NUMERO = "numero";
    String DATA = "data";
    String BOOLEANO = "booleano";

    Map<String, Hl7TypeConverter> REGISTRY = initRegistry();

    static Map<String, Hl7TypeConverter> initRegistry() {
        Map<String, Hl7TypeConverter> map = new ConcurrentHashMap<>();

        Hl7TypeConverter textConverter = rawValue -> {
            if (rawValue == null || rawValue.isEmpty()) {
                return null;
            }
            return rawValue;
        };

        Hl7TypeConverter numberConverter = rawValue -> {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                return null;
            }
            String clean = rawValue.trim();
            if (clean.indexOf(',') != -1 && clean.indexOf('.') == -1) {
                clean = clean.replace(',', '.');
            }
            try {
                if (clean.indexOf('.') == -1 && clean.indexOf('e') == -1 && clean.indexOf('E') == -1) {
                    return Long.valueOf(clean);
                } else {
                    return Double.valueOf(clean);
                }
            } catch (NumberFormatException e) {
                throw new Hl7MappingException("Cannot convert value '" + rawValue + "' to number", e);
            }
        };

        Hl7TypeConverter dateConverter = rawValue -> {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                return null;
            }
            try {
                return Hl7DateTime.parse(rawValue.trim());
            } catch (Exception e) {
                throw new Hl7MappingException("Cannot convert value '" + rawValue + "' to date/time", e);
            }
        };

        Hl7TypeConverter booleanConverter = rawValue -> {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                return null;
            }
            String clean = rawValue.trim().toUpperCase(Locale.ROOT);
            if ("Y".equals(clean) || "YES".equals(clean) || "T".equals(clean) || "TRUE".equals(clean)
                    || "1".equals(clean) || "S".equals(clean) || "SIM".equals(clean)) {
                return Boolean.TRUE;
            }
            if ("N".equals(clean) || "NO".equals(clean) || "F".equals(clean) || "FALSE".equals(clean)
                    || "0".equals(clean) || "NAO".equals(clean) || "NÃO".equals(clean)) {
                return Boolean.FALSE;
            }
            throw new Hl7MappingException("Cannot convert value '" + rawValue + "' to boolean");
        };

        map.put("texto", textConverter);
        map.put("text", textConverter);
        map.put("string", textConverter);

        map.put("numero", numberConverter);
        map.put("number", numberConverter);

        map.put("data", dateConverter);
        map.put("date", dateConverter);
        map.put("datetime", dateConverter);

        map.put("booleano", booleanConverter);
        map.put("boolean", booleanConverter);
        map.put("bool", booleanConverter);

        return map;
    }

    static void registerConverter(String name, Hl7TypeConverter converter) {
        if (name == null || name.trim().isEmpty() || converter == null) {
            return;
        }
        REGISTRY.put(name.trim().toLowerCase(Locale.ROOT), converter);
    }

    static Hl7TypeConverter getConverter(String name) {
        if (name == null) {
            return null;
        }
        return REGISTRY.get(name.trim().toLowerCase(Locale.ROOT));
    }

    static boolean hasConverter(String name) {
        if (name == null) {
            return false;
        }
        return REGISTRY.containsKey(name.trim().toLowerCase(Locale.ROOT));
    }

    static Map<String, Hl7TypeConverter> getRegisteredConverters() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
