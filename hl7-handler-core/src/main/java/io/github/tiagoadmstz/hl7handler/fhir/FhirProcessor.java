package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.fhir.mapping.FhirToMapTransformer;
import io.github.tiagoadmstz.hl7handler.fhir.parser.FhirParser;

import java.io.InputStream;
import java.util.Map;

/**
 * Immutable and thread-safe FHIR processor, created by {@code Hl7Handler.fhir().build()}.
 */
public final class FhirProcessor {

    private final FhirParser parser;
    private final FhirToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final Map<String, Object> mapping;

    public FhirProcessor(
        FhirParser parser,
        FhirToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        Map<String, Object> mapping
    ) {
        this.parser = parser;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mapping = mapping != null ? FhirResult.deepCopy(mapping) : null;
    }

    public FhirResult read(String json) {
        FhirResource resource = parser.parse(json);
        return new FhirResult(resource, transformer, jsonBinder, mapping);
    }

    public FhirResult read(InputStream in) {
        FhirResource resource = parser.parse(in);
        return new FhirResult(resource, transformer, jsonBinder, mapping);
    }

    public FhirResult readBytes(byte[] bytes) {
        FhirResource resource = parser.parse(bytes);
        return new FhirResult(resource, transformer, jsonBinder, mapping);
    }

    public boolean hasMapping() {
        return mapping != null;
    }
}
