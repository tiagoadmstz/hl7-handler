package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.fhir.mapping.FhirToMapTransformer;
import io.github.tiagoadmstz.hl7handler.fhir.parser.FhirParser;
import io.github.tiagoadmstz.hl7handler.fhir.writer.FhirMapToJsonTransformer;
import io.github.tiagoadmstz.hl7handler.fhir.writer.FhirWriter;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TypeConverter;
import io.github.tiagoadmstz.hl7handler.mapping.YamlMappingLoader;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent builder for {@link FhirProcessor}, obtained from {@code Hl7Handler.fhir()}.
 */
public final class FhirBuilder {

    private Map<String, Object> mappingConfig;
    private File mappingFile;
    private Path mappingPath;
    private InputStream mappingStream;
    private String mappingContent;
    private boolean keepEmptyStrings;
    private boolean indent = true;
    private FhirVersion defaultVersion = FhirVersion.UNKNOWN;
    private final Map<String, Hl7TypeConverter> converters = new LinkedHashMap<>();

    public FhirBuilder indent(boolean indent) {
        this.indent = indent;
        return this;
    }

    public FhirBuilder withMapping(File file) {
        this.mappingFile = file;
        return this;
    }

    public FhirBuilder withMapping(Path path) {
        this.mappingPath = path;
        return this;
    }

    public FhirBuilder withMapping(InputStream inputStream) {
        this.mappingStream = inputStream;
        return this;
    }

    public FhirBuilder withMapping(String content) {
        this.mappingContent = content;
        return this;
    }

    public FhirBuilder withMapping(Map<String, Object> mapping) {
        this.mappingConfig = mapping;
        return this;
    }

    public FhirBuilder keepEmptyStrings(boolean keepEmptyStrings) {
        this.keepEmptyStrings = keepEmptyStrings;
        return this;
    }

    public FhirBuilder defaultVersion(FhirVersion version) {
        if (version != null) {
            this.defaultVersion = version;
        }
        return this;
    }

    public FhirBuilder converter(String name, Hl7TypeConverter converter) {
        if (name != null && converter != null) {
            converters.put(name.toLowerCase(), converter);
        }
        return this;
    }

    public static FhirVersion detectVersion(String json) {
        if (json == null || json.trim().isEmpty()) {
            return FhirVersion.UNKNOWN;
        }
        try {
            JsonNode root = JsonMapper.builder().build().readTree(json);
            return FhirVersion.detect(root);
        } catch (Exception e) {
            return FhirVersion.UNKNOWN;
        }
    }

    public FhirProcessor build() {
        Map<String, Object> loadedMapping = loadMapping();
        JsonMapper jsonMapper = JsonMapper.builder().build();
        FhirParser parser = new FhirParser(jsonMapper, defaultVersion);
        FhirToMapTransformer transformer = new FhirToMapTransformer(keepEmptyStrings, converters, jsonMapper);
        Hl7JsonBinder jsonBinder = new Hl7JsonBinder(jsonMapper);

        return new FhirProcessor(parser, transformer, jsonBinder, loadedMapping);
    }

    public FhirWriter buildWriter() {
        Map<String, Object> loadedMapping = loadMapping();
        JsonMapper jsonMapper = JsonMapper.builder().build();
        FhirVersion resolved = (defaultVersion != null && defaultVersion != FhirVersion.UNKNOWN)
            ? defaultVersion
            : FhirVersion.R4;
        FhirMapToJsonTransformer transformer = new FhirMapToJsonTransformer(jsonMapper, resolved);
        Hl7JsonBinder jsonBinder = new Hl7JsonBinder(jsonMapper);

        return new FhirWriter(transformer, loadedMapping, indent, jsonMapper, jsonBinder, resolved);
    }

    public FhirResource convert(Hl7Message message) {
        return buildWriter().convert(message);
    }

    public FhirResource convert(String rawHl7) {
        return buildWriter().convert(rawHl7);
    }

    private Map<String, Object> loadMapping() {
        if (mappingConfig != null) {
            return mappingConfig;
        }
        YAMLMapper yamlMapper = YAMLMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
        YamlMappingLoader loader = new YamlMappingLoader(yamlMapper);
        try {
            if (mappingFile != null) {
                return loader.load(mappingFile);
            }
            if (mappingPath != null) {
                return loader.load(mappingPath);
            }
            if (mappingStream != null) {
                return loader.load(mappingStream);
            }
            if (mappingContent != null) {
                return loader.load(new java.io.ByteArrayInputStream(mappingContent.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            throw new Hl7MappingException("Error loading FHIR mapping: " + e.getMessage(), e);
        }
        return null;
    }
}
