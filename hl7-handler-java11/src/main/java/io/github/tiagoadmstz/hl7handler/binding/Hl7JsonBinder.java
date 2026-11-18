package io.github.tiagoadmstz.hl7handler.binding;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.github.tiagoadmstz.hl7handler.Hl7BindingException;
import io.github.tiagoadmstz.hl7handler.model.Hl7Field;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.model.Hl7Segment;

import java.io.IOException;
import java.time.temporal.Temporal;
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
        SimpleModule timeModule = new SimpleModule();
        timeModule.addSerializer(Temporal.class, new JsonSerializer<Temporal>() {
            @Override
            public void serialize(Temporal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value != null ? value.toString() : null);
            }
        });
        this.jsonMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .addModule(timeModule)
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
        } catch (JsonProcessingException e) {
            throw new Hl7BindingException("Error serializing map to JSON", e);
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
        } catch (IllegalArgumentException e) {
            throw new Hl7BindingException("Error binding mapped values to " + targetClass.getName(), e);
        }
    }

    /**
     * Deserializes a JSON string into the client's custom Value Object.
     */
    public <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return jsonMapper.readValue(json, targetClass);
        } catch (JsonProcessingException e) {
            throw new Hl7BindingException("Error deserializing JSON to " + targetClass.getName(), e);
        }
    }
}
