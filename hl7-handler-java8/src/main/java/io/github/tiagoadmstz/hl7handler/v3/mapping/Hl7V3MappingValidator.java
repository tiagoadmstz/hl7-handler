package io.github.tiagoadmstz.hl7handler.v3.mapping;

import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TypeConverter;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates an HL7 v3 mapping on build, so that a bad XPath, an unknown converter or a wrong format are reported
 * with the YAML path of the node. A {@code #name} coordinate is not checked here: it is resolved against the
 * strategy of each document at read time.
 */
public final class Hl7V3MappingValidator {

    private Hl7V3MappingValidator() {}

    /**
     * Validates the mapping and throws on the first invalid node.
     *
     * @param mapping the loaded YAML mapping; {@code null} is accepted and skipped
     * @param knownConverters names of custom converters, lower-case; may be {@code null}
     * @param namespaces the namespace declarations used to compile the XPath expressions; may be {@code null}
     * @throws Hl7MappingException with the YAML path of the first invalid node
     */
    public static void validate(Map<String, Object> mapping, Set<String> knownConverters, Map<String, String> namespaces) {
        if (mapping == null) {
            return;
        }
        validateFormat(mapping);
        XPath xpath = new Hl7V3Namespaces(namespaces).newXPath();
        Set<String> converters = knownConverters != null ? knownConverters : Collections.<String>emptySet();
        String prefix = mapping.containsKey("mappings") ? "mappings" : "";
        validateTemplate(Hl7V3ToMapTransformer.templateOf(mapping), prefix, converters, xpath);
    }

    private static void validateFormat(Map<String, Object> mapping) {
        Object format = Hl7V3ToMapTransformer.formatOf(mapping);
        if (format == null) {
            return;
        }
        if (Hl7V3Format.fromName(String.valueOf(format)) == null) {
            throw new Hl7MappingException("Invalid format '" + format + "' at 'formato': expected CDA or MESSAGE");
        }
    }

    private static void validateTemplate(Map<?, ?> template, String path, Set<String> converters, XPath xpath) {
        for (Map.Entry<?, ?> entry : template.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String childPath = path.isEmpty() ? key : path + "." + key;
            validateNode(entry.getValue(), childPath, converters, xpath);
        }
    }

    private static void validateNode(Object node, String path, Set<String> converters, XPath xpath) {
        if (node instanceof String) {
            validateXPath(expressionOf(node, path), path, xpath);
            return;
        }
        if (!(node instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> map = (Map<?, ?>) node;
        Hl7V3DirectiveKind kind = Hl7V3DirectiveKind.classify(map);
        if (kind == Hl7V3DirectiveKind.LIST) {
            Object spec = map.containsKey("lista") ? map.get("lista") : map.get("list");
            validateXPath(expressionOf(spec, path), path, xpath);
            Object item = map.get("item");
            if (!(item instanceof Map<?, ?>)) {
                throw new Hl7MappingException("Missing 'item' map for lista at '" + path + "'");
            }
            validateTemplate((Map<?, ?>) item, path + ".item", converters, xpath);
            return;
        }
        if (kind == Hl7V3DirectiveKind.CONSTANT) {
            Object constantXpath = map.containsKey("xpath") ? map.get("xpath") : map.get("path");
            if (constantXpath != null) {
                validateXPath(expressionOf(constantXpath, path), path, xpath);
            }
            validateType(map, path, converters);
            return;
        }
        if (kind == Hl7V3DirectiveKind.COORDINATE) {
            Object xpathValue = map.containsKey("xpath") ? map.get("xpath") : map.get("path");
            validateXPath(expressionOf(xpathValue, path), path, xpath);
            Object fallback = map.get("fallback");
            if (fallback != null) {
                validateXPath(expressionOf(fallback, path + ".fallback"), path + ".fallback", xpath);
            }
            validateType(map, path, converters);
            return;
        }
        validateTemplate(map, path, converters, xpath);
    }

    private static String expressionOf(Object raw, String path) {
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            throw new Hl7MappingException("Empty XPath at '" + path + "'");
        }
        return String.valueOf(raw).trim();
    }

    private static void validateXPath(String expression, String path, XPath xpath) {
        if (expression.startsWith("#")) {
            return;
        }
        try {
            xpath.compile(expression);
        } catch (XPathExpressionException e) {
            throw new Hl7MappingException("Invalid XPath '" + expression + "' at '" + path + "'", e);
        }
    }

    private static void validateType(Map<?, ?> map, String path, Set<String> converters) {
        Object type = map.containsKey("tipo") ? map.get("tipo") : map.get("type");
        if (type == null) {
            return;
        }
        String name = String.valueOf(type).trim();
        if (name.isEmpty()) {
            return;
        }
        String lookupName = name.toLowerCase(Locale.ROOT);
        if (converters.contains(lookupName) || Hl7TypeConverter.getConverter(lookupName) != null) {
            return;
        }
        throw new Hl7MappingException("Unknown type converter '" + name + "' at '" + path + "'");
    }
}
