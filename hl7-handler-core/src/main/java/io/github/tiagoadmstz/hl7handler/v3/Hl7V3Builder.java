package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7TypeConverter;
import io.github.tiagoadmstz.hl7handler.mapping.YamlMappingLoader;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3MappingValidator;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3ToMapTransformer;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Parser;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fluent builder for an {@link Hl7V3Processor}, obtained from {@code Hl7Handler.v3()}. The YAML mapping is loaded
 * and validated on {@link #build()}.
 */
public final class Hl7V3Builder {

    private Map<String, Object> mappingConfig;
    private File mappingFile;
    private Path mappingPath;
    private InputStream mappingStream;
    private String mappingContent;
    private boolean keepEmptyStrings;
    private Hl7V3StrategyRegistry strategyRegistry;
    private final Map<String, Hl7TypeConverter> converters = new LinkedHashMap<>();
    private final Map<String, String> namespaces = new LinkedHashMap<>();

    /**
     * Creates a builder with no mapping, the default strategy registry and empty strings replaced by {@code null}.
     */
    public Hl7V3Builder() {}

    /**
     * Keeps empty values as empty strings instead of {@code null} in the mapped output.
     *
     * @param keepEmptyStrings whether to keep empty strings
     * @return this builder
     */
    public Hl7V3Builder keepEmptyStrings(boolean keepEmptyStrings) {
        this.keepEmptyStrings = keepEmptyStrings;
        return this;
    }

    /**
     * Registers a custom converter usable as {@code tipo} in the mapping. Ignored when the name or converter is null or blank.
     *
     * @param name the converter name, case-insensitive
     * @param converter the converter
     * @return this builder
     */
    public Hl7V3Builder converter(String name, Hl7TypeConverter converter) {
        if (name != null && !name.trim().isEmpty() && converter != null) {
            converters.put(name.trim().toLowerCase(Locale.ROOT), converter);
        }
        return this;
    }

    /**
     * Declares an extra XPath prefix. The prefixes {@code hl7}, {@code sdtc} and {@code xsi} are built in; a prefix
     * from the mapping's {@code namespaces} block is overridden by this call.
     *
     * @param prefix the prefix used in XPath expressions
     * @param uri the namespace URI
     * @return this builder
     */
    public Hl7V3Builder namespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
        return this;
    }

    /**
     * Sets the strategy registry. Defaults to {@link Hl7V3StrategyRegistry#defaultRegistry()} when not set.
     *
     * @param registry the strategy registry; {@code null} keeps the default
     * @return this builder
     */
    public Hl7V3Builder strategyRegistry(Hl7V3StrategyRegistry registry) {
        if (registry != null) {
            this.strategyRegistry = registry;
        }
        return this;
    }

    /**
     * Uses a YAML mapping file.
     *
     * @param file the mapping file
     * @return this builder
     */
    public Hl7V3Builder withMapping(File file) {
        this.mappingFile = file;
        return this;
    }

    /**
     * Uses a YAML mapping file.
     *
     * @param path the mapping path
     * @return this builder
     */
    public Hl7V3Builder withMapping(Path path) {
        this.mappingPath = path;
        return this;
    }

    /**
     * Uses a YAML mapping read from a stream. The stream is consumed on {@link #build()}.
     *
     * @param inputStream the mapping stream
     * @return this builder
     */
    public Hl7V3Builder withMapping(InputStream inputStream) {
        this.mappingStream = inputStream;
        return this;
    }

    /**
     * Uses a YAML mapping given as text.
     *
     * @param content the YAML text
     * @return this builder
     */
    public Hl7V3Builder withMappingContent(String content) {
        this.mappingContent = content;
        return this;
    }

    /**
     * Uses a mapping already loaded as a Map, validated the same way as a YAML file.
     *
     * @param mapping the mapping structure
     * @return this builder
     */
    public Hl7V3Builder withMapping(Map<String, Object> mapping) {
        this.mappingConfig = mapping;
        return this;
    }

    /**
     * Loads and validates the mapping, then creates the processor.
     *
     * @return an immutable, thread-safe processor
     * @throws Hl7MappingException if the mapping cannot be read, or a node is invalid, with its YAML path
     */
    public Hl7V3Processor build() {
        YamlMappingLoader yamlLoader = newYamlLoader();
        Map<String, Object> mapping = readMappingSource(yamlLoader);
        Map<String, String> effectiveNamespaces = effectiveNamespaces(mapping);
        if (mapping != null) {
            Set<String> converterNames = new HashSet<>(converters.keySet());
            Hl7V3MappingValidator.validate(mapping, converterNames, effectiveNamespaces);
        }
        Hl7V3StrategyRegistry registry = strategyRegistry != null ? strategyRegistry : Hl7V3StrategyRegistry.defaultRegistry();
        Hl7V3Parser parser = new Hl7V3Parser(registry, new Hl7V3Namespaces(effectiveNamespaces));
        Hl7V3ToMapTransformer transformer = new Hl7V3ToMapTransformer(keepEmptyStrings, converters);
        return new Hl7V3Processor(parser, transformer, new Hl7JsonBinder(), mapping);
    }

    private YamlMappingLoader newYamlLoader() {
        YAMLMapper yamlMapper = YAMLMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build();
        return new YamlMappingLoader(yamlMapper);
    }

    private Map<String, String> effectiveNamespaces(Map<String, Object> mapping) {
        Map<String, String> result = new LinkedHashMap<>();
        Object declared = mapping != null ? mapping.get("namespaces") : null;
        if (declared != null) {
            if (!(declared instanceof Map<?, ?>)) {
                throw new Hl7MappingException("Invalid 'namespaces' at 'namespaces': expected a map of prefix to URI");
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) declared).entrySet()) {
                Object uri = entry.getValue();
                result.put(String.valueOf(entry.getKey()), uri == null ? "" : String.valueOf(uri));
            }
        }
        result.putAll(namespaces);
        return result;
    }

    private Map<String, Object> readMappingSource(YamlMappingLoader yamlLoader) {
        if (mappingConfig != null) {
            return mappingConfig;
        }
        try {
            if (mappingFile != null) {
                return yamlLoader.load(mappingFile);
            }
            if (mappingPath != null) {
                return yamlLoader.load(mappingPath);
            }
            if (mappingStream != null) {
                return yamlLoader.load(mappingStream);
            }
            if (mappingContent != null) {
                return yamlLoader.load(mappingContent);
            }
        } catch (IOException e) {
            throw new Hl7MappingException("Error reading YAML mapping", e);
        }
        return null;
    }
}
