package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.Hl7BindingException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable wrapper over a FHIR JSON resource tree.
 */
public final class FhirResource {

    private static final Pattern INDEX_PATTERN = Pattern.compile("^(.*?)\\[(\\d+|\\*)\\]$");

    private final JsonNode root;
    private final FhirVersion version;
    private final JsonMapper jsonMapper;

    public FhirResource(JsonNode root, FhirVersion version, JsonMapper jsonMapper) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Root FHIR node must be a non-null JSON object");
        }
        this.root = root;
        this.version = version != null ? version : FhirVersion.UNKNOWN;
        this.jsonMapper = jsonMapper != null ? jsonMapper : JsonMapper.builder().build();
    }

    public JsonNode rootNode() {
        return root;
    }

    public FhirVersion version() {
        return version;
    }

    public String resourceType() {
        JsonNode node = root.get("resourceType");
        return node != null && node.isTextual() ? node.asText() : "";
    }

    public String id() {
        JsonNode node = root.get("id");
        return node != null && node.isTextual() ? node.asText() : "";
    }

    public String versionId() {
        JsonNode meta = root.get("meta");
        if (meta != null && meta.isObject()) {
            JsonNode v = meta.get("versionId");
            if (v != null && v.isTextual()) {
                return v.asText();
            }
        }
        return "";
    }

    public boolean isBundle() {
        return "Bundle".equals(resourceType());
    }

    /**
     * Resolves a single scalar value from a property path (e.g. {@code "code.coding[0].code"}).
     * Returns an empty string if the path does not exist.
     */
    public String get(String path) {
        List<String> values = getAll(path);
        return values.isEmpty() ? "" : values.get(0);
    }

    /**
     * Resolves all matching values for the specified path, supporting wildcard indexes (e.g. {@code "entry[*].resource.id"}).
     */
    public List<String> getAll(String path) {
        if (path == null || path.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String cleanPath = path.trim();
        if (cleanPath.startsWith("$.")) {
            cleanPath = cleanPath.substring(2);
        }
        String[] segments = cleanPath.split("\\.");
        List<JsonNode> current = Collections.singletonList(root);

        for (String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }
            List<JsonNode> next = new ArrayList<>();
            Matcher matcher = INDEX_PATTERN.matcher(segment);
            if (matcher.matches()) {
                String fieldName = matcher.group(1);
                String indexStr = matcher.group(2);
                for (JsonNode node : current) {
                    JsonNode target = fieldName.isEmpty() ? node : node.get(fieldName);
                    if (target != null && target.isArray()) {
                        if ("*".equals(indexStr)) {
                            for (JsonNode item : target) {
                                next.add(item);
                            }
                        } else {
                            int idx = Integer.parseInt(indexStr);
                            if (idx >= 0 && idx < target.size()) {
                                next.add(target.get(idx));
                            }
                        }
                    }
                }
            } else {
                for (JsonNode node : current) {
                    if (node != null && node.isObject()) {
                        JsonNode child = node.get(segment);
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

        List<String> results = new ArrayList<>(current.size());
        for (JsonNode node : current) {
            if (node.isValueNode()) {
                results.add(node.asText());
            } else {
                results.add(node.toString());
            }
        }
        return Collections.unmodifiableList(results);
    }

    public String toJson() {
        try {
            return jsonMapper.writeValueAsString(root);
        } catch (JacksonException e) {
            throw new Hl7BindingException("Failed to serialize FHIR resource to JSON", e);
        }
    }

    public String toPrettyJson() {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JacksonException e) {
            throw new Hl7BindingException("Failed to serialize FHIR resource to pretty JSON", e);
        }
    }
}
