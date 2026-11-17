package io.github.tdteixeira.hl7handler.mapping;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates a loaded YAML mapping before it is used, so that coordinates, list specs, type names and
 * version ranges that the transformer would silently resolve to empty values are reported on load,
 * with the YAML path of the offending node. Uses the same directive classification as the transformer.
 */
public final class Hl7MappingValidator {

    private static final Pattern SEMANTIC_COORDINATE = Pattern.compile("(?i)(@event|@patientId|EVENT|PATIENT_ID)");
    private static final Pattern ABSOLUTE_COORDINATE = Pattern.compile(
            "(?i)[A-Z][A-Z0-9]{2}(\\[\\d+\\])?(([.-]\\d+)(\\[(\\*|\\d+)\\])?([.-]\\d+){0,2})?");
    private static final Pattern RELATIVE_COORDINATE = Pattern.compile("\\d+([.-]\\d+){0,2}");
    private static final Pattern LIST_SPEC = Pattern.compile("(?i)[A-Z][A-Z0-9]{2}(\\[\\*\\]|[.-]\\d+(\\[\\*\\])?)?");

    private Hl7MappingValidator() {}

    /**
     * Validates the mapping structure and throws if any node is invalid.
     *
     * @param mapping the YAML mapping structure; {@code null} is accepted and skipped
     * @param knownConverters names of custom converters registered on the transformer, lower-cased
     * @throws Hl7MappingException with the YAML path of the first invalid node
     */
    public static void validate(Map<String, Object> mapping, Set<String> knownConverters) {
        if (mapping == null) {
            return;
        }
        Set<String> converters = knownConverters != null ? knownConverters : Collections.<String>emptySet();

        Object versoes = Hl7ToMapTransformer.versionConstraintOf(mapping);
        if (versoes != null && !String.valueOf(versoes).trim().isEmpty()) {
            validateVersionRange(String.valueOf(versoes).trim(), "versoes");
        }

        Map<String, Object> baseTemplate = Hl7ToMapTransformer.rawBaseTemplate(mapping);
        validateTemplate(baseTemplate, "", converters, false);

        Map<?, ?> overlays = Hl7ToMapTransformer.overlaysOf(mapping);
        if (overlays == null) {
            return;
        }
        for (Map.Entry<?, ?> entry : overlays.entrySet()) {
            String range = String.valueOf(entry.getKey()).trim();
            validateVersionRange(range, "porVersao[" + range + "]");
            if (!(entry.getValue() instanceof Map<?, ?>)) {
                continue;
            }
            Map<?, ?> body = (Map<?, ?>) entry.getValue();
            Map<String, Object> merged = Hl7ToMapTransformer.deepCopyMap(baseTemplate);
            Hl7ToMapTransformer.deepMerge(merged, body);
            for (Object key : body.keySet()) {
                String name = String.valueOf(key);
                validateNode(merged.get(name), "porVersao[" + range + "]." + name, converters, false);
            }
        }
    }

    private static void validateTemplate(Map<?, ?> template, String path, Set<String> converters, boolean inListItem) {
        for (Map.Entry<?, ?> entry : template.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String childPath = path.isEmpty() ? key : path + "." + key;
            validateNode(entry.getValue(), childPath, converters, inListItem);
        }
    }

    private static void validateNode(Object node, String path, Set<String> converters, boolean inListItem) {
        if (node instanceof String) {
            validateCoordinate((String) node, path, inListItem);
            return;
        }
        if (!(node instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> map = (Map<?, ?>) node;
        Hl7DirectiveKind kind = Hl7DirectiveKind.classify(map);

        if (kind == Hl7DirectiveKind.LIST) {
            Object spec = map.containsKey("lista") ? map.get("lista") : map.get("list");
            validateListSpec(String.valueOf(spec).trim(), path);
            Object item = map.get("item");
            if (item instanceof Map<?, ?>) {
                validateTemplate((Map<?, ?>) item, path + ".item", converters, true);
            } else if (item != null) {
                validateNode(item, path + ".item", converters, true);
            }
            return;
        }

        if (kind == Hl7DirectiveKind.CONSTANT) {
            if (map.containsKey("tipo") || map.containsKey("type")) {
                validateConverter(typeOf(map), path, converters);
            }
            return;
        }

        if (kind == Hl7DirectiveKind.COORDINATE) {
            validateCoordinate(String.valueOf(coordinateOf(map)), path, inListItem);
            if (map.containsKey("fallback") || map.containsKey("padrao")) {
                Object fallback = map.containsKey("fallback") ? map.get("fallback") : map.get("padrao");
                if (fallback instanceof String && Hl7ToMapTransformer.HL7_COORDINATE_PATTERN.matcher((String) fallback).matches()) {
                    validateCoordinate((String) fallback, path, inListItem);
                }
            }
            if (map.containsKey("tipo") || map.containsKey("type")) {
                validateConverter(typeOf(map), path, converters);
            }
            return;
        }

        validateTemplate(map, path, converters, inListItem);
    }

    private static Object coordinateOf(Map<?, ?> map) {
        if (map.containsKey("hl7")) {
            return map.get("hl7");
        }
        if (map.containsKey("path")) {
            return map.get("path");
        }
        return map.get("campo");
    }

    private static Object typeOf(Map<?, ?> map) {
        return map.containsKey("tipo") ? map.get("tipo") : map.get("type");
    }

    private static void validateCoordinate(String raw, String path, boolean inListItem) {
        String clean = raw.trim();
        if (clean.isEmpty()
                || SEMANTIC_COORDINATE.matcher(clean).matches()
                || ABSOLUTE_COORDINATE.matcher(clean).matches()
                || (inListItem && RELATIVE_COORDINATE.matcher(clean).matches())) {
            return;
        }
        throw new Hl7MappingException("Invalid HL7 coordinate '" + clean + "' at '" + path + "'");
    }

    private static void validateListSpec(String spec, String path) {
        if (!LIST_SPEC.matcher(spec).matches()) {
            throw new Hl7MappingException("Invalid HL7 list '" + spec + "' at '" + path + "'");
        }
    }

    private static void validateConverter(Object typeObj, String path, Set<String> converters) {
        String name = String.valueOf(typeObj).trim();
        if (name.isEmpty()) {
            return;
        }
        String lookupName = name.toLowerCase(Locale.ROOT);
        if (converters.contains(lookupName) || Hl7TypeConverter.getConverter(lookupName) != null) {
            return;
        }
        throw new Hl7MappingException("Unknown type converter '" + name + "' at '" + path + "'");
    }

    private static void validateVersionRange(String expression, String path) {
        try {
            Hl7VersionRange.parse(expression);
        } catch (Hl7MappingException e) {
            throw new Hl7MappingException("Invalid version range '" + expression + "' at '" + path + "': " + e.getMessage(), e);
        }
    }
}
