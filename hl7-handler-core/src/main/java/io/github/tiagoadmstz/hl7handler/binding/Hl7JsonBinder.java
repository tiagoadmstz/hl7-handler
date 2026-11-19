package io.github.tiagoadmstz.hl7handler.binding;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.SerializationFeature;

import io.github.tiagoadmstz.hl7handler.Hl7BindingException;
import io.github.tiagoadmstz.hl7handler.model.Hl7Field;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.model.Hl7Segment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles JSON serialization and Value Object (VO) data binding.
 */
public class Hl7JsonBinder {

    private final JsonMapper jsonMapper;

    public Hl7JsonBinder() {
        this.jsonMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    public Hl7JsonBinder(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public JsonMapper getObjectMapper() {
        return jsonMapper;
    }

    /**
     * Converts a semantic Map (e.g. from YAML mapping) to formatted JSON string.
     */
    public String toJson(Map<String, Object> data) {
        try {
            return jsonMapper.writeValueAsString(data);
        } catch (JacksonException e) {
            throw new Hl7BindingException("Error serializing map to JSON", e);
        }
    }

    /**
     * Converts a Value Object, or any bean-like object, into a Map keyed by property name, so that it can be written
     * with a YAML mapping. A Map value is returned as an equivalent Map. Returns {@code null} for a {@code null} value.
     *
     * @throws Hl7BindingException if the object cannot be converted
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toMap(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return jsonMapper.convertValue(value, Map.class);
        } catch (IllegalArgumentException | JacksonException e) {
            throw new Hl7BindingException("Error converting " + value.getClass().getName() + " to a map", e);
        }
    }

    /**
     * Converts an Hl7Message to raw structural JSON representation.
     */
    public String toRawJson(Hl7Message message) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", message.getVersion().getIdentifier());

        List<Map<String, Object>> segList = new ArrayList<>();
        for (Hl7Segment seg : message.getSegments()) {
            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("segment", seg.getName());

            List<Object> fieldVals = new ArrayList<>();
            for (Hl7Field field : seg.getFields()) {
                if (field.getRepetitions().size() > 1) {
                    fieldVals.add(field.getValues());
                } else {
                    fieldVals.add(field.getValue());
                }
            }
            sMap.put("fields", fieldVals);
            segList.add(sMap);
        }
        root.put("segments", segList);

        return toJson(root);
    }

    /**
     * Binds a semantic Map into the client's custom Value Object (VO / DTO / POJO).
     */
    public <T> T bind(Map<String, Object> data, Class<T> targetClass) {
        try {
            return jsonMapper.convertValue(data, targetClass);
        } catch (IllegalArgumentException | JacksonException e) {
            throw new Hl7BindingException("Error binding mapped values to " + targetClass.getName(), e);
        }
    }

    /**
     * Deserializes a JSON string into the client's custom Value Object.
     */
    public <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return jsonMapper.readValue(json, targetClass);
        } catch (JacksonException e) {
            throw new Hl7BindingException("Error deserializing JSON to " + targetClass.getName(), e);
        }
    }
}
