package io.github.tiagoadmstz.hl7handler.v3.mapping;

import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TypeConverter;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Transforms an HL7 v3 document into a semantic Map according to a YAML mapping. Every string is an XPath relative
 * to the current context node; the {@code lista} directive turns each selected node into the context of its
 * {@code item} template. An empty value becomes {@code null}, or an empty string when empty strings are kept.
 */
public final class Hl7V3ToMapTransformer {

    private static final String MAPPINGS_KEY = "mappings";
    private static final Set<String> METADATA_KEYS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("formato", "format", "namespaces", "description", "version", MAPPINGS_KEY)));

    private final boolean keepEmptyStrings;
    private final Map<String, Hl7TypeConverter> customConverters;

    /**
     * Creates a transformer.
     *
     * @param keepEmptyStrings whether empty values become {@code ""} instead of {@code null}
     * @param customConverters converters usable as {@code tipo}, keyed by lower-case name; may be {@code null}
     */
    public Hl7V3ToMapTransformer(boolean keepEmptyStrings, Map<String, Hl7TypeConverter> customConverters) {
        this.keepEmptyStrings = keepEmptyStrings;
        Map<String, Hl7TypeConverter> copy = new LinkedHashMap<>();
        if (customConverters != null) {
            copy.putAll(customConverters);
        }
        this.customConverters = Collections.unmodifiableMap(copy);
    }

    /**
     * Applies the mapping to the document.
     *
     * @param document the HL7 v3 document
     * @param mappingConfig the loaded YAML mapping
     * @return a new mutable map; the caller copies it when it needs immutability
     * @throws Hl7MappingException if the mapping format differs from the document format, or an XPath or converter fails
     */
    public Map<String, Object> transform(Hl7V3Document document, Map<String, Object> mappingConfig) {
        Object declared = formatOf(mappingConfig);
        if (declared != null) {
            Hl7V3Format mappingFormat = Hl7V3Format.fromName(String.valueOf(declared));
            if (mappingFormat == null) {
                throw new Hl7MappingException("Invalid format '" + declared + "' at 'formato': expected CDA or MESSAGE");
            }
            if (mappingFormat != document.format()) {
                throw new Hl7MappingException("Document format '" + document.format() + "' does not match mapping format '"
                    + mappingFormat + "'");
            }
        }
        return evaluateTemplate(document, null, templateOf(mappingConfig));
    }

    /**
     * Returns the template of a mapping: the {@code mappings} block when present, otherwise every top-level key
     * that is not metadata.
     */
    static Map<String, Object> templateOf(Map<String, Object> mappingConfig) {
        Map<String, Object> template = new LinkedHashMap<>();
        if (mappingConfig.containsKey(MAPPINGS_KEY)) {
            Object mappings = mappingConfig.get(MAPPINGS_KEY);
            if (mappings == null) {
                return template;
            }
            if (!(mappings instanceof Map<?, ?>)) {
                throw new Hl7MappingException("Invalid mapping at 'mappings': expected a map");
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) mappings).entrySet()) {
                template.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return template;
        }
        for (Map.Entry<String, Object> entry : mappingConfig.entrySet()) {
            if (!METADATA_KEYS.contains(entry.getKey())) {
                template.put(entry.getKey(), entry.getValue());
            }
        }
        return template;
    }

    /**
     * Returns the raw value of the {@code formato} key, or of {@code format} when that one is absent.
     */
    static Object formatOf(Map<String, Object> mappingConfig) {
        if (mappingConfig.containsKey("formato")) {
            return mappingConfig.get("formato");
        }
        return mappingConfig.get("format");
    }

    private Map<String, Object> evaluateTemplate(Hl7V3Document document, Node context, Map<?, ?> template) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : template.entrySet()) {
            result.put(String.valueOf(entry.getKey()), evaluateNode(document, context, entry.getValue()));
        }
        return result;
    }

    private Object evaluateNode(Hl7V3Document document, Node context, Object node) {
        if (node instanceof String) {
            return applyTypeConversion(null, document.selectText((String) node, context));
        }
        if (!(node instanceof Map<?, ?>)) {
            return node;
        }
        Map<?, ?> map = (Map<?, ?>) node;
        Hl7V3DirectiveKind kind = Hl7V3DirectiveKind.classify(map);
        if (kind == Hl7V3DirectiveKind.LIST) {
            return evaluateList(document, context, map);
        }
        if (kind == Hl7V3DirectiveKind.CONSTANT) {
            return evaluateConstant(map);
        }
        if (kind == Hl7V3DirectiveKind.COORDINATE) {
            return evaluateCoordinate(document, context, map);
        }
        return evaluateTemplate(document, context, map);
    }

    private List<Object> evaluateList(Hl7V3Document document, Node context, Map<?, ?> map) {
        Object spec = map.containsKey("lista") ? map.get("lista") : map.get("list");
        Object item = map.get("item");
        if (!(item instanceof Map<?, ?>)) {
            throw new Hl7MappingException("Missing 'item' map for lista '" + spec + "'");
        }
        List<Object> values = new ArrayList<>();
        for (Node itemNode : document.selectNodes(String.valueOf(spec), context)) {
            values.add(evaluateTemplate(document, itemNode, (Map<?, ?>) item));
        }
        return values;
    }

    private Object evaluateConstant(Map<?, ?> map) {
        Object value = map.containsKey("valor") ? map.get("valor") : map.get("value");
        String typeName = typeNameOf(map);
        if (typeName == null) {
            return value;
        }
        return applyTypeConversion(typeName, value == null ? "" : String.valueOf(value));
    }

    private Object evaluateCoordinate(Hl7V3Document document, Node context, Map<?, ?> map) {
        Object xpath = map.containsKey("xpath") ? map.get("xpath") : map.get("path");
        String raw = document.selectText(String.valueOf(xpath), context);
        Object fallback = map.get("fallback");
        if (raw.isEmpty() && fallback != null) {
            raw = document.selectText(String.valueOf(fallback), context);
        }
        Object padrao = map.get("padrao");
        if (raw.isEmpty() && padrao != null) {
            raw = String.valueOf(padrao);
        }
        return applyTypeConversion(typeNameOf(map), raw);
    }

    private static String typeNameOf(Map<?, ?> map) {
        Object type = map.containsKey("tipo") ? map.get("tipo") : map.get("type");
        if (type == null) {
            return null;
        }
        return String.valueOf(type);
    }

    private Object applyTypeConversion(String typeName, String raw) {
        if (raw.isEmpty()) {
            if (keepEmptyStrings) {
                return "";
            }
            return null;
        }
        if (typeName == null || typeName.trim().isEmpty()) {
            return raw;
        }
        String cleanType = typeName.trim().toLowerCase(Locale.ROOT);
        Hl7TypeConverter converter = customConverters.get(cleanType);
        if (converter == null) {
            converter = Hl7TypeConverter.getConverter(cleanType);
        }
        if (converter == null) {
            throw new Hl7MappingException("Unknown type converter: '" + typeName + "'");
        }
        return converter.convert(raw);
    }
}
