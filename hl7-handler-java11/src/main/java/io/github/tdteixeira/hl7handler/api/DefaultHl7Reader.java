package io.github.tdteixeira.hl7handler.api;

import io.github.tdteixeira.hl7handler.binding.Hl7JsonBinder;
import io.github.tdteixeira.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tdteixeira.hl7handler.mapping.YamlMappingLoader;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.network.MllpStreamReader;
import io.github.tdteixeira.hl7handler.parser.Hl7Parser;
import io.github.tdteixeira.hl7handler.parser.Hl7ParserRegistry;
import io.github.tdteixeira.hl7handler.parser.Hl7VersionDetector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Default implementation of the fluent Hl7Reader API.
 */
public class DefaultHl7Reader implements Hl7Reader {

    private String rawHl7;
    private Map<String, Object> mappingConfig;

    private final Hl7ParserRegistry registry;
    private final YamlMappingLoader yamlLoader;
    private final Hl7ToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final MllpStreamReader mllpReader;

    /**
    * Private constructor to prevent direct instantiation without dependencies.
    */
    private DefaultHl7Reader() {
        this.registry = null;
        this.yamlLoader = null;
        this.transformer = null;
        this.jsonBinder = null;
        this.mllpReader = null;
    }

    public DefaultHl7Reader(
        Hl7ParserRegistry registry,
        YamlMappingLoader yamlLoader,
        Hl7ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        MllpStreamReader mllpReader
    ) {
        this.registry = registry;
        this.yamlLoader = yamlLoader;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mllpReader = mllpReader;
    }

    @Override
    public Hl7Reader from(String hl7Text) {
        this.rawHl7 = hl7Text;
        return this;
    }

    @Override
    public Hl7Reader fromBytes(byte[] bytes) {
        this.rawHl7 = mllpReader.fromBytes(bytes);
        return this;
    }

    @Override
    public Hl7Reader fromMllp(InputStream inputStream) {
        try {
            this.rawHl7 = mllpReader.readMessage(inputStream);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading MLLP byte stream", e);
        }
    }

    @Override
    public Hl7Reader withMapping(File yamlFile) {
        try {
            this.mappingConfig = yamlLoader.load(yamlFile);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading YAML mapping file: " + yamlFile, e);
        }
    }

    @Override
    public Hl7Reader withMapping(Path yamlPath) {
        try {
            this.mappingConfig = yamlLoader.load(yamlPath);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading YAML mapping path: " + yamlPath, e);
        }
    }

    @Override
    public Hl7Reader withMapping(InputStream yamlInputStream) {
        try {
            this.mappingConfig = yamlLoader.load(yamlInputStream);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading YAML mapping stream", e);
        }
    }

    @Override
    public Hl7Reader withMappingContent(String yamlContent) {
        try {
            this.mappingConfig = yamlLoader.load(yamlContent);
            return this;
        } catch (IOException e) {
            throw new UncheckedIOException("Error parsing YAML content", e);
        }
    }

    @Override
    public Hl7Message toMessage() {
        if (rawHl7 == null || rawHl7.trim().isEmpty()) {
            throw new IllegalStateException("HL7 input content must be provided via from(), fromBytes() or fromMllp()");
        }
        Hl7Version version = Hl7VersionDetector.detect(rawHl7);
        Hl7Parser parser = registry.getParserFor(version);
        return parser.parse(rawHl7);
    }

    @Override
    public String toRawJson() {
        return jsonBinder.toRawJson(toMessage());
    }

    @Override
    public String toJson() {
        if (mappingConfig != null) {
            return jsonBinder.toJson(toMap());
        }
        return toRawJson();
    }

    @Override
    public Map<String, Object> toMap() {
        if (mappingConfig == null) {
            throw new IllegalStateException("No YAML mapping was configured. Call withMapping() before toMap() or use toRawJson()");
        }
        return transformer.transform(toMessage(), mappingConfig);
    }

    @Override
    public <T> T bindTo(Class<T> targetClass) {
        Map<String, Object> map = toMap();
        return jsonBinder.bind(map, targetClass);
    }
}
