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
import io.github.tiagoadmstz.hl7handler.v3.writer.Hl7V3MapToXmlTransformer;
import io.github.tiagoadmstz.hl7handler.v3.writer.Hl7V3Writer;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fluent builder for an {@link Hl7V3Processor} or an {@link Hl7V3Writer}, obtained from {@code Hl7Handler.v3()}. The
 * YAML mapping is loaded and validated on {@link #build()} or {@link #buildWriter()}. A schema, when set, is compiled
 * once by the build method, and its {@code include} and {@code import} references are resolved from the schema's own
 * location, from the file system or the classpath only.
 */
public final class Hl7V3Builder {

    private static final String WRITER_MAPPING_REQUIRED =
        "An HL7 v3 writer needs a YAML mapping: call withMapping() before buildWriter()";

    private Map<String, Object> mappingConfig;
    private File mappingFile;
    private Path mappingPath;
    private InputStream mappingStream;
    private String mappingContent;
    private boolean keepEmptyStrings;
    private Hl7V3StrategyRegistry strategyRegistry;
    private final Map<String, Hl7TypeConverter> converters = new LinkedHashMap<>();
    private final Map<String, String> namespaces = new LinkedHashMap<>();
    private Path templatePath;
    private File templateFile;
    private InputStream templateStream;
    private String templateContent;
    private boolean indent = true;
    private StreamSource schemaSource;
    private Hl7V3SchemaMode schemaMode = Hl7V3SchemaMode.STRICT;

    /**
     * Creates a builder with no mapping, the default strategy registry, empty strings replaced by {@code null} and
     * indented writer output.
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
     * Uses an XML template file for the writer. Its root element gives the root of every written document, and it must
     * be in the HL7 v3 namespace. Takes precedence over {@link #templateContent(String)} when both are set.
     *
     * @param path the template file
     * @return this builder
     */
    public Hl7V3Builder template(Path path) {
        this.templatePath = path;
        return this;
    }

    /**
     * Uses an XML template file for the writer. See {@link #template(Path)}.
     *
     * @param file the template file
     * @return this builder
     */
    public Hl7V3Builder template(File file) {
        this.templateFile = file;
        return this;
    }

    /**
     * Uses an XML template read from a stream. The stream is consumed on {@link #buildWriter()}.
     *
     * @param inputStream the template stream
     * @return this builder
     */
    public Hl7V3Builder template(InputStream inputStream) {
        this.templateStream = inputStream;
        return this;
    }

    /**
     * Uses an XML template given as text. The text is encoded as UTF-8 before it is parsed, so its XML declaration
     * should name UTF-8 or no encoding.
     *
     * @param content the XML text
     * @return this builder
     */
    public Hl7V3Builder templateContent(String content) {
        this.templateContent = content;
        return this;
    }

    /**
     * Sets whether the writer indents its output by two spaces per level. Defaults to {@code true}.
     *
     * @param indent whether to indent
     * @return this builder
     */
    public Hl7V3Builder indent(boolean indent) {
        this.indent = indent;
        return this;
    }

    /**
     * Validates documents against an XSD schema file. Ignored when the path is {@code null}.
     *
     * @param path the schema file
     * @return this builder
     */
    public Hl7V3Builder schema(Path path) {
        if (path != null) {
            this.schemaSource = new StreamSource(path.toFile());
        }
        return this;
    }

    /**
     * Validates documents against an XSD schema file. Ignored when the file is {@code null}.
     *
     * @param file the schema file
     * @return this builder
     */
    public Hl7V3Builder schema(File file) {
        if (file != null) {
            this.schemaSource = new StreamSource(file);
        }
        return this;
    }

    /**
     * Validates documents against an XSD schema given by URL. Only {@code file} and {@code jar} URLs are read, so the
     * network is never accessed. Ignored when the URL is {@code null}.
     *
     * @param url the schema URL
     * @return this builder
     */
    public Hl7V3Builder schema(URL url) {
        if (url != null) {
            this.schemaSource = new StreamSource(url.toExternalForm());
        }
        return this;
    }

    /**
     * Sets how an XSD violation is reported. Defaults to {@link Hl7V3SchemaMode#STRICT}.
     *
     * @param mode the mode; {@code null} keeps STRICT
     * @return this builder
     */
    public Hl7V3Builder schemaValidation(Hl7V3SchemaMode mode) {
        this.schemaMode = mode == null ? Hl7V3SchemaMode.STRICT : mode;
        return this;
    }

    /**
     * Loads and validates the mapping, then creates the processor.
     *
     * @return an immutable, thread-safe processor
     * @throws Hl7MappingException if the mapping cannot be read, or a node is invalid, with its YAML path
     * @throws IllegalArgumentException if the configured schema cannot be loaded
     */
    public Hl7V3Processor build() {
        YamlMappingLoader yamlLoader = newYamlLoader();
        Map<String, Object> mapping = readMappingSource(yamlLoader);
        Map<String, String> effectiveNamespaces = effectiveNamespaces(mapping);
        if (mapping != null) {
            Set<String> converterNames = new HashSet<>(converters.keySet());
            Hl7V3MappingValidator.validate(mapping, converterNames, effectiveNamespaces);
        }
        Hl7V3StrategyRegistry registry = registry();
        Hl7V3Namespaces namespaces = new Hl7V3Namespaces(effectiveNamespaces);
        Hl7V3Parser parser = new Hl7V3Parser(registry, namespaces, compileSchema(), schemaMode);
        Hl7V3ToMapTransformer transformer = new Hl7V3ToMapTransformer(keepEmptyStrings, converters);
        return new Hl7V3Processor(parser, transformer, new Hl7JsonBinder(), mapping);
    }

    /**
     * Loads and validates the mapping and the template, then creates the writer. Every XPath of the mapping is checked
     * for writability here, so an error is reported before any document is written.
     *
     * @return an immutable, thread-safe writer
     * @throws Hl7MappingException if there is no mapping, a path is not writable, the template is invalid, or the root
     *     element cannot be determined from a template or a {@code raiz} key
     * @throws IllegalArgumentException if the configured schema cannot be loaded
     */
    public Hl7V3Writer buildWriter() {
        YamlMappingLoader yamlLoader = newYamlLoader();
        Map<String, Object> mapping = readMappingSource(yamlLoader);
        if (mapping == null) {
            throw new Hl7MappingException(WRITER_MAPPING_REQUIRED);
        }
        Map<String, String> effectiveNamespaces = effectiveNamespaces(mapping);
        Set<String> converterNames = new HashSet<>(converters.keySet());
        Hl7V3MappingValidator.validate(mapping, converterNames, effectiveNamespaces);
        Hl7V3StrategyRegistry registry = registry();
        Hl7V3Namespaces namespaces = new Hl7V3Namespaces(effectiveNamespaces);
        Schema schema = compileSchema();
        Hl7V3MapToXmlTransformer transformer = new Hl7V3MapToXmlTransformer(
            Hl7V3Result.deepCopy(mapping), readTemplate(), namespaces, registry);
        Hl7V3Parser schemaValidator = schema == null ? null : new Hl7V3Parser(registry, namespaces, schema, schemaMode);
        return new Hl7V3Writer(transformer, schemaValidator, indent, new Hl7JsonBinder());
    }

    private Hl7V3StrategyRegistry registry() {
        return strategyRegistry != null ? strategyRegistry : Hl7V3StrategyRegistry.defaultRegistry();
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

    private byte[] readTemplate() {
        try {
            if (templatePath != null) {
                return Files.readAllBytes(templatePath);
            }
            if (templateFile != null) {
                return Files.readAllBytes(templateFile.toPath());
            }
            if (templateStream != null) {
                return readAll(templateStream);
            }
            if (templateContent != null) {
                return templateContent.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new Hl7MappingException("Error reading the HL7 v3 writer template", e);
        }
        return null;
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private Schema compileSchema() {
        if (schemaSource == null) {
            return null;
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file,jar");
            factory.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // A warning in the schema does not stop the schema from being used.
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            return factory.newSchema(schemaSource);
        } catch (SAXException e) {
            throw new IllegalArgumentException("Cannot load XSD schema: " + e.getMessage(), e);
        }
    }
}
