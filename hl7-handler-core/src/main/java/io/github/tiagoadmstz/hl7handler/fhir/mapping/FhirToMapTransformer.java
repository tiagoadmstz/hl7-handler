package io.github.tiagoadmstz.hl7handler.fhir.mapping;

import io.github.tiagoadmstz.hl7handler.fhir.FhirResource;
import io.github.tiagoadmstz.hl7handler.fhir.FhirVersion;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TypeConverter;
import io.github.tiagoadmstz.hl7handler.model.Hl7DateTime;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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
 * Transforms a FHIR resource tree into a semantic Map according to a YAML mapping.
 */
public final class FhirToMapTransformer {

    private static final String MAPPINGS_KEY = "mappings";
    private static final Set<String> METADATA_KEYS = Collections.unmodifiableSet(new HashSet<>(
        Arrays.asList("familia", "family", "recurso", "resource", "versao", "version", "description",
            "porVersao", "byVersion", MAPPINGS_KEY)));

    private final boolean keepEmptyStrings;
    private final Map<String, Hl7TypeConverter> customConverters;
    private final JsonMapper jsonMapper;

    public FhirToMapTransformer(boolean keepEmptyStrings, Map<String, Hl7TypeConverter> customConverters, JsonMapper jsonMapper) {
        this.keepEmptyStrings = keepEmptyStrings;
        Map<String, Hl7TypeConverter> copy = new LinkedHashMap<>();
        if (customConverters != null) {
            copy.putAll(customConverters);
        }
        this.customConverters = Collections.unmodifiableMap(copy);
        this.jsonMapper = jsonMapper != null ? jsonMapper : JsonMapper.builder().build();
    }

    public Map<String, Object> transform(FhirResource resource, Map<String, Object> mappingConfig) {
        if (mappingConfig == null || mappingConfig.isEmpty()) {
            return Collections.emptyMap();
        }
        validateResourceMatch(resource, mappingConfig);
        Map<String, Object> mergedTemplate = buildMergedTemplate(resource.version(), mappingConfig);
        return evaluateTemplate(resource, resource.rootNode(), mergedTemplate);
    }

    private void validateResourceMatch(FhirResource resource, Map<String, Object> mappingConfig) {
        Object expectedResource = mappingConfig.get("recurso");
        if (expectedResource == null) {
            expectedResource = mappingConfig.get("resource");
        }
        if (expectedResource != null) {
            String exp = String.valueOf(expectedResource).trim();
            if (!exp.equalsIgnoreCase(resource.resourceType())) {
                throw new Hl7MappingException("Expected FHIR resource '" + exp + "' does not match actual resource '"
                    + resource.resourceType() + "'");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildMergedTemplate(FhirVersion version, Map<String, Object> mappingConfig) {
        Map<String, Object> baseTemplate = extractTemplate(mappingConfig);
        Object versionOverrides = mappingConfig.get("porVersao");
        if (versionOverrides == null) {
            versionOverrides = mappingConfig.get("byVersion");
        }
        if (!(versionOverrides instanceof Map<?, ?>)) {
            return baseTemplate;
        }

        Map<?, ?> overrides = (Map<?, ?>) versionOverrides;
        String vKey = version != null ? version.name() : "";
        Object specific = overrides.get(vKey);
        if (specific == null && version != null) {
            specific = overrides.get(vKey.toLowerCase(Locale.ROOT));
        }
        if (!(specific instanceof Map<?, ?>)) {
            return baseTemplate;
        }

        Map<String, Object> merged = new LinkedHashMap<>(baseTemplate);
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) specific).entrySet()) {
            merged.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return merged;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractTemplate(Map<String, Object> mappingConfig) {
        Object mappings = mappingConfig.get(MAPPINGS_KEY);
        if (mappings instanceof Map<?, ?>) {
            return (Map<String, Object>) mappings;
        }
        Map<String, Object> template = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : mappingConfig.entrySet()) {
            if (!METADATA_KEYS.contains(entry.getKey())) {
                template.put(entry.getKey(), entry.getValue());
            }
        }
        return template;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluateTemplate(FhirResource resource, JsonNode contextNode, Map<String, Object> template) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object rule = entry.getValue();
            Object value = evaluateRule(resource, contextNode, rule);
            if (value != null || keepEmptyStrings) {
                result.put(key, value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object evaluateRule(FhirResource resource, JsonNode contextNode, Object rule) {
        if (rule instanceof String) {
            return evaluatePath(resource, contextNode, (String) rule, null);
        }
        if (rule instanceof Map<?, ?>) {
            Map<String, Object> map = (Map<String, Object>) rule;
            if (map.containsKey("lista") || map.containsKey("list")) {
                return evaluateList(resource, contextNode, map);
            }
            if (map.containsKey("valor") || map.containsKey("value")) {
                Object val = map.containsKey("valor") ? map.get("valor") : map.get("value");
                String tipo = extractString(map, "tipo", "type");
                return applyType(val, tipo);
            }
            String path = extractString(map, "path", "de");
            if (path != null) {
                String fallback = extractString(map, "fallback");
                String padrao = extractString(map, "padrao", "default");
                String tipo = extractString(map, "tipo", "type");
                Object val = evaluatePath(resource, contextNode, path, fallback);
                if (val == null && padrao != null) {
                    val = padrao;
                }
                return applyType(val, tipo);
            }
            return evaluateTemplate(resource, contextNode, (Map<String, Object>) rule);
        }
        return rule;
    }

    @SuppressWarnings("unchecked")
    private List<Object> evaluateList(FhirResource resource, JsonNode contextNode, Map<String, Object> listRule) {
        String listPath = extractString(listRule, "lista", "list");
        Object itemTemplate = listRule.get("item");
        if (listPath == null || !(itemTemplate instanceof Map<?, ?>)) {
            return Collections.emptyList();
        }

        List<JsonNode> matchingNodes = resolveNodes(contextNode != null ? contextNode : resource.rootNode(), listPath);
        List<Object> results = new ArrayList<>();
        for (JsonNode node : matchingNodes) {
            Map<String, Object> itemMap = evaluateTemplate(resource, node, (Map<String, Object>) itemTemplate);
            if (!itemMap.isEmpty()) {
                results.add(itemMap);
            }
        }
        return Collections.unmodifiableList(results);
    }

    private Object evaluatePath(FhirResource resource, JsonNode contextNode, String path, String fallback) {
        String val = resolveSingleValue(contextNode != null ? contextNode : resource.rootNode(), path);
        if ((val == null || val.isEmpty()) && fallback != null) {
            val = resolveSingleValue(contextNode != null ? contextNode : resource.rootNode(), fallback);
        }
        if (val == null || val.isEmpty()) {
            return keepEmptyStrings ? "" : null;
        }
        return val;
    }

    private String resolveSingleValue(JsonNode baseNode, String path) {
        List<JsonNode> nodes = resolveNodes(baseNode, path);
        if (nodes.isEmpty()) {
            return null;
        }
        JsonNode first = nodes.get(0);
        return first.isValueNode() ? first.asText() : first.toString();
    }

    private List<JsonNode> resolveNodes(JsonNode baseNode, String path) {
        if (baseNode == null || path == null || path.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String clean = path.trim();
        if (clean.startsWith("$.")) {
            clean = clean.substring(2);
        }
        String[] parts = clean.split("\\.");
        List<JsonNode> current = Collections.singletonList(baseNode);

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            List<JsonNode> next = new ArrayList<>();
            int lBracket = part.indexOf('[');
            if (lBracket >= 0 && part.endsWith("]")) {
                String field = part.substring(0, lBracket);
                String idxStr = part.substring(lBracket + 1, part.length() - 1);
                for (JsonNode n : current) {
                    JsonNode target = field.isEmpty() ? n : n.get(field);
                    if (target != null && target.isArray()) {
                        if ("*".equals(idxStr)) {
                            for (JsonNode item : target) {
                                next.add(item);
                            }
                        } else {
                            try {
                                int i = Integer.parseInt(idxStr);
                                if (i >= 0 && i < target.size()) {
                                    next.add(target.get(i));
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            } else {
                for (JsonNode n : current) {
                    if (n != null && n.isObject()) {
                        JsonNode child = n.get(part);
                        if (child != null && !child.isNull()) {
                            next.add(child);
                        }
                    }
                }
            }
            current = next;
            if (current.isEmpty()) {
                break;
            }
        }
        return current;
    }

    private Object applyType(Object value, String type) {
        if (value == null) {
            return null;
        }
        if (type == null || type.trim().isEmpty()) {
            return value;
        }
        String t = type.trim().toLowerCase(Locale.ROOT);
        String str = String.valueOf(value).trim();
        if ("texto".equals(t) || "string".equals(t)) {
            return str;
        }
        if ("numero".equals(t) || "number".equals(t)) {
            try {
                if (str.contains(".")) {
                    return Double.parseDouble(str);
                }
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                return str;
            }
        }
        if ("booleano".equals(t) || "boolean".equals(t)) {
            return Boolean.parseBoolean(str);
        }
        if ("data".equals(t) || "date".equals(t) || "datetime".equals(t)) {
            return Hl7DateTime.parse(str);
        }
        Hl7TypeConverter custom = customConverters.get(t);
        if (custom != null) {
            return custom.convert(str);
        }
        return value;
    }

    private static String extractString(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                return String.valueOf(v).trim();
            }
        }
        return null;
    }
}
