package io.github.tiagoadmstz.hl7handler.fhir.writer;

import io.github.tiagoadmstz.hl7handler.Hl7BindingException;
import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.fhir.FhirResource;
import io.github.tiagoadmstz.hl7handler.fhir.FhirVersion;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Immutable and thread-safe FHIR writer and converter, created by {@code Hl7Handler.fhir().buildWriter()}.
 */
public final class FhirWriter {

    private final FhirMapToJsonTransformer transformer;
    private final Map<String, Object> mappingConfig;
    private final boolean indent;
    private final JsonMapper jsonMapper;
    private final Hl7JsonBinder jsonBinder;
    private final FhirVersion version;

    public FhirWriter(
        FhirMapToJsonTransformer transformer,
        Map<String, Object> mappingConfig,
        boolean indent,
        JsonMapper jsonMapper,
        Hl7JsonBinder jsonBinder,
        FhirVersion version
    ) {
        this.transformer = transformer;
        this.mappingConfig = mappingConfig;
        this.indent = indent;
        this.jsonMapper = jsonMapper != null ? jsonMapper : JsonMapper.builder().build();
        this.jsonBinder = jsonBinder != null ? jsonBinder : new Hl7JsonBinder(this.jsonMapper);
        this.version = version != null ? version : FhirVersion.R4;
    }

    public String write(Map<String, Object> data) {
        if (data == null) {
            throw new IllegalArgumentException("Data to write must not be null");
        }
        ObjectNode node = transformer.transform(data, mappingConfig);
        return serialize(node);
    }

    public String write(Object vo) {
        if (vo == null) {
            throw new IllegalArgumentException("VO to write must not be null");
        }
        return write(jsonBinder.toMap(vo));
    }

    public byte[] writeBytes(Map<String, Object> data) {
        return write(data).getBytes(StandardCharsets.UTF_8);
    }

    public byte[] writeBytes(Object vo) {
        return write(vo).getBytes(StandardCharsets.UTF_8);
    }

    public FhirResource convert(Hl7Message message) {
        if (message == null) {
            throw new IllegalArgumentException("HL7 v2 message to convert must not be null");
        }
        ObjectNode root = transformer.transformFromV2(message, mappingConfig);
        return new FhirResource(root, version, jsonMapper);
    }

    public FhirResource convert(String rawHl7) {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            throw new IllegalArgumentException("Raw HL7 v2 string must not be null or blank");
        }
        Hl7Message message = Hl7Handler.reader().from(rawHl7).toMessage();
        return convert(message);
    }

    public FhirResource convertBytes(byte[] rawHl7) {
        if (rawHl7 == null || rawHl7.length == 0) {
            throw new IllegalArgumentException("Raw HL7 v2 bytes must not be null or empty");
        }
        return convert(new String(rawHl7, StandardCharsets.UTF_8));
    }

    private String serialize(ObjectNode node) {
        try {
            if (indent) {
                return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            }
            return jsonMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new Hl7BindingException("Failed to serialize FHIR JSON: " + e.getMessage(), e);
        }
    }
}

