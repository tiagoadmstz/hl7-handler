package io.github.tiagoadmstz.hl7handler.fhir.writer;

import io.github.tiagoadmstz.hl7handler.fhir.FhirUnits;
import io.github.tiagoadmstz.hl7handler.fhir.FhirVersion;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.model.Hl7DateTime;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.model.Hl7Segment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforms an input Map (or an HL7 v2 message) into a FHIR JSON resource tree according to YAML mapping rules.
 */
public final class FhirMapToJsonTransformer {

    private static final Pattern PATH_INDEX_PATTERN = Pattern.compile("^(.*?)\\[(\\d+)\\]$");

    private final JsonMapper jsonMapper;
    private final FhirVersion version;

    public FhirMapToJsonTransformer(JsonMapper jsonMapper, FhirVersion version) {
        this.jsonMapper = jsonMapper != null ? jsonMapper : JsonMapper.builder().build();
        this.version = version != null ? version : FhirVersion.R4;
    }

    public ObjectNode transform(Map<String, Object> data, Map<String, Object> mappingConfig) {
        if (mappingConfig == null || mappingConfig.isEmpty()) {
            throw new Hl7MappingException("FHIR mapping configuration must not be null or empty");
        }
        ObjectNode root = jsonMapper.createObjectNode();
        String resourceType = extractString(mappingConfig, "recurso", "resource");
        if (resourceType != null) {
            root.put("resourceType", resourceType);
        }
        Map<String, Object> montagem = extractMontagem(mappingConfig);
        for (Map.Entry<String, Object> entry : montagem.entrySet()) {
            Object value = resolveValue(entry.getValue(), data, null, null);
            if (value != null) {
                setPathValue(root, entry.getKey(), value);
            }
        }
        return root;
    }

    public ObjectNode transformFromV2(Hl7Message message, Map<String, Object> mappingConfig) {
        if (mappingConfig == null || mappingConfig.isEmpty()) {
            throw new Hl7MappingException("FHIR mapping configuration must not be null or empty");
        }
        String resourceType = extractString(mappingConfig, "recurso", "resource");
        if ("Bundle".equalsIgnoreCase(resourceType)) {
            return buildBundleFromV2(message, mappingConfig);
        }

        ObjectNode root = jsonMapper.createObjectNode();
        if (resourceType != null) {
            root.put("resourceType", resourceType);
        }
        Map<String, Object> montagem = extractMontagem(mappingConfig);
        for (Map.Entry<String, Object> entry : montagem.entrySet()) {
            Object value = resolveValue(entry.getValue(), Collections.emptyMap(), message, null);
            if (value != null) {
                setPathValue(root, entry.getKey(), value);
            }
        }
        return root;
    }

    private ObjectNode buildBundleFromV2(Hl7Message message, Map<String, Object> mappingConfig) {
        ObjectNode bundle = jsonMapper.createObjectNode();
        bundle.put("resourceType", "Bundle");
        bundle.put("id", "bundle-" + UUID.randomUUID());
        String bundleType = extractString(mappingConfig, "tipo", "type");
        bundle.put("type", bundleType != null ? bundleType : "transaction");

        ArrayNode entries = bundle.putArray("entry");

        // 1. Patient entry if referenced
        String patientUrn = "urn:uuid:" + UUID.randomUUID();
        String patientId = message.get("PID.3.1");
        if (patientId != null && !patientId.isEmpty()) {
            ObjectNode patEntry = entries.addObject();
            patEntry.put("fullUrl", patientUrn);
            ObjectNode patient = patEntry.putObject("resource");
            patient.put("resourceType", "Patient");
            patient.put("id", "pat-" + patientId);
            ArrayNode identifiers = patient.putArray("identifier");
            ObjectNode idNode = identifiers.addObject();
            idNode.put("value", patientId);
            String patientName = message.get("PID.5.1");
            if (patientName != null && !patientName.isEmpty()) {
                ArrayNode names = patient.putArray("name");
                names.addObject().put("family", patientName);
            }
            if ("transaction".equalsIgnoreCase(bundle.get("type").asText())) {
                ObjectNode req = patEntry.putObject("request");
                req.put("method", "POST");
                req.put("url", "Patient");
            }
        }

        // 2. Device entry if present
        String deviceUrn = "urn:uuid:" + UUID.randomUUID();
        String deviceId = null;
        for (Hl7Segment obx : message.getSegments("OBX")) {
            String val18 = obx.getComponentValue(18, 1);
            if (val18 == null || val18.isEmpty()) {
                val18 = obx.getFieldValue(18);
            }
            if (val18 != null && !val18.isEmpty()) {
                deviceId = val18;
                break;
            }
        }
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = message.get("MSH.3.1");
        }
        if (deviceId != null && !deviceId.isEmpty()) {
            ObjectNode devEntry = entries.addObject();
            devEntry.put("fullUrl", deviceUrn);
            ObjectNode device = devEntry.putObject("resource");
            device.put("resourceType", "Device");
            device.put("id", "dev-" + deviceId.replaceAll("[^a-zA-Z0-9.-]", ""));
            ArrayNode idArr = device.putArray("identifier");
            idArr.addObject().put("value", deviceId);
            if ("transaction".equalsIgnoreCase(bundle.get("type").asText())) {
                ObjectNode req = devEntry.putObject("request");
                req.put("method", "POST");
                req.put("url", "Device");
            }
        }

        // 3. Observations from OBX segments
        List<Hl7Segment> obxList = message.getSegments("OBX");
        Map<String, Object> montagem = extractMontagem(mappingConfig);

        int obsIndex = 1;
        for (Hl7Segment obx : obxList) {
            String valueType = obx.getFieldValue(2);
            String val = obx.getFieldValue(5);
            if (val == null || val.isEmpty()) {
                continue;
            }
            ObjectNode obsEntry = entries.addObject();
            String obsUrn = "urn:uuid:" + UUID.randomUUID();
            obsEntry.put("fullUrl", obsUrn);

            ObjectNode obs = obsEntry.putObject("resource");
            obs.put("resourceType", "Observation");
            obs.put("id", "obs-" + (obsIndex++));
            obs.put("status", "final");

            // Subject reference
            if (patientId != null && !patientId.isEmpty()) {
                obs.putObject("subject").put("reference", patientUrn);
            }

            // Device reference (adapting R4 vs R5)
            if (deviceId != null && !deviceId.isEmpty()) {
                if (version == FhirVersion.R5) {
                    obs.putObject("device").putObject("reference").put("reference", deviceUrn);
                } else {
                    obs.putObject("device").put("reference", deviceUrn);
                }
            }

            // Default mapping from OBX
            String code = obx.getComponentValue(3, 1);
            String display = obx.getComponentValue(3, 2);
            if (code != null && !code.isEmpty()) {
                ObjectNode codeObj = obs.putObject("code");
                ArrayNode coding = codeObj.putArray("coding");
                ObjectNode c = coding.addObject();
                c.put("system", "urn:iso:std:iso:11073:10101");
                c.put("code", code);
                if (display != null && !display.isEmpty()) {
                    c.put("display", display);
                    codeObj.put("text", display);
                }
            }

            // Value Quantity
            String rawUnit = obx.getFieldValue(6);
            String ucum = FhirUnits.toUcum(rawUnit);
            String unit = ucum != null ? ucum : (rawUnit != null ? rawUnit : "");
            ObjectNode vq = obs.putObject("valueQuantity");
            try {
                if (val.contains(".")) {
                    vq.put("value", Double.parseDouble(val));
                } else {
                    vq.put("value", Long.parseLong(val));
                }
            } catch (NumberFormatException e) {
                vq.put("value", val);
            }
            if (!unit.isEmpty()) {
                vq.put("unit", unit);
                vq.put("system", "http://unitsofmeasure.org");
                vq.put("code", unit);
            }

            // Effective Date Time
            String obsTime = obx.getFieldValue(14);
            if (obsTime == null || obsTime.isEmpty()) {
                obsTime = message.get("MSH.7.1");
            }
            if (obsTime != null && !obsTime.isEmpty()) {
                try {
                    TemporalAccessor parsed = Hl7DateTime.parse(obsTime);
                    obs.put("effectiveDateTime", parsed.toString());
                } catch (Exception ignored) {
                    obs.put("effectiveDateTime", obsTime);
                }
            }

            // Custom montagem overlays per OBX if defined
            for (Map.Entry<String, Object> entry : montagem.entrySet()) {
                Object customVal = resolveValue(entry.getValue(), Collections.emptyMap(), message, obx);
                if (customVal != null) {
                    setPathValue(obs, entry.getKey(), customVal);
                }
            }

            if ("transaction".equalsIgnoreCase(bundle.get("type").asText())) {
                ObjectNode req = obsEntry.putObject("request");
                req.put("method", "POST");
                req.put("url", "Observation");
            }
        }

        return bundle;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractMontagem(Map<String, Object> mappingConfig) {
        Object m = mappingConfig.get("montagem");
        if (m == null) {
            m = mappingConfig.get("mappings");
        }
        Map<String, Object> base = new LinkedHashMap<>();
        if (m instanceof Map<?, ?>) {
            base.putAll((Map<String, Object>) m);
        }

        // Apply version override
        Object byVersion = mappingConfig.get("porVersao");
        if (byVersion == null) {
            byVersion = mappingConfig.get("byVersion");
        }
        if (byVersion instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) byVersion;
            Object specific = map.get(version.name());
            if (specific == null) {
                specific = map.get(version.name().toLowerCase(Locale.ROOT));
            }
            if (specific instanceof Map<?, ?>) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) specific).entrySet()) {
                    base.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        return base;
    }

    private Object resolveValue(Object rule, Map<String, Object> data, Hl7Message message, Hl7Segment segment) {
        if (rule instanceof String) {
            String str = (String) rule;
            if (str.startsWith("$.")) {
                return data.get(str.substring(2));
            }
            if (str.startsWith("v2:")) {
                return resolveV2Coordinate(str.substring(3), message, segment);
            }
            return str;
        }
        if (rule instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) rule;
            if (map.containsKey("valor")) {
                return map.get("valor");
            }
            if (map.containsKey("value")) {
                return map.get("value");
            }
            Object de = map.containsKey("de") ? map.get("de") : map.get("path");
            String tipo = extractString(map, "tipo", "type");
            Object val = null;
            if (de != null) {
                val = resolveValue(de, data, message, segment);
            }
            if (val == null && map.containsKey("fallback")) {
                val = resolveValue(map.get("fallback"), data, message, segment);
            }
            if (val == null && map.containsKey("padrao")) {
                val = map.get("padrao");
            }
            return applyType(val, tipo);
        }
        return rule;
    }

    private Object resolveV2Coordinate(String coordinate, Hl7Message message, Hl7Segment segment) {
        if (coordinate == null || coordinate.isEmpty()) {
            return null;
        }
        if (coordinate.startsWith("OBX.") && segment != null) {
            String[] parts = coordinate.split("\\.");
            int field = Integer.parseInt(parts[1]);
            if (parts.length == 2) {
                return segment.getFieldValue(field);
            }
            if (parts.length >= 3) {
                int comp = Integer.parseInt(parts[2]);
                return segment.getComponentValue(field, comp);
            }
        }
        if (message != null) {
            return message.get(coordinate);
        }
        return null;
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
        if ("ucum".equals(t)) {
            String ucum = FhirUnits.toUcum(str);
            return ucum != null ? ucum : str;
        }
        if ("data".equals(t) || "datetime".equals(t)) {
            try {
                TemporalAccessor accessor = Hl7DateTime.parse(str);
                return accessor.toString();
            } catch (Exception ignored) {
                return str;
            }
        }
        return str;
    }

    private void setPathValue(ObjectNode root, String path, Object value) {
        String[] parts = path.split("\\.");
        JsonNode current = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Matcher matcher = PATH_INDEX_PATTERN.matcher(part);
            if (matcher.matches()) {
                String field = matcher.group(1);
                int idx = Integer.parseInt(matcher.group(2));
                ObjectNode objNode = (ObjectNode) current;
                ArrayNode array = objNode.has(field) && objNode.get(field).isArray()
                    ? (ArrayNode) objNode.get(field)
                    : objNode.putArray(field);
                while (array.size() <= idx) {
                    array.addObject();
                }
                current = array.get(idx);
            } else {
                ObjectNode objNode = (ObjectNode) current;
                current = objNode.has(part) && objNode.get(part).isObject()
                    ? objNode.get(part)
                    : objNode.putObject(part);
            }
        }

        String lastPart = parts[parts.length - 1];
        Matcher matcher = PATH_INDEX_PATTERN.matcher(lastPart);
        if (matcher.matches()) {
            String field = matcher.group(1);
            int idx = Integer.parseInt(matcher.group(2));
            ObjectNode objNode = (ObjectNode) current;
            ArrayNode array = objNode.has(field) && objNode.get(field).isArray()
                ? (ArrayNode) objNode.get(field)
                : objNode.putArray(field);
            while (array.size() <= idx) {
                array.addObject();
            }
            setLeafValue((ObjectNode) array.get(idx), "", value);
        } else {
            setLeafValue((ObjectNode) current, lastPart, value);
        }
    }

    private void setLeafValue(ObjectNode target, String fieldName, Object value) {
        if (fieldName.isEmpty()) {
            return;
        }
        if (value instanceof Number) {
            if (value instanceof Double || value instanceof Float) {
                target.put(fieldName, ((Number) value).doubleValue());
            } else {
                target.put(fieldName, ((Number) value).longValue());
            }
        } else if (value instanceof Boolean) {
            target.put(fieldName, (Boolean) value);
        } else {
            target.put(fieldName, String.valueOf(value));
        }
    }

    private static String extractString(Map<?, ?> map, String... keys) {
        for (String k : keys) {
            Object v = map.get(k);
            if (v != null) {
                return String.valueOf(v).trim();
            }
        }
        return null;
    }
}

