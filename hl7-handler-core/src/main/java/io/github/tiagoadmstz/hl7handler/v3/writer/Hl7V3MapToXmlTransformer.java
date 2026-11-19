package io.github.tiagoadmstz.hl7handler.v3.writer;

import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3DirectiveKind;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3ToMapTransformer;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Xml;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import io.github.tiagoadmstz.hl7handler.v3.version.cda.CdaR2Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.message.Hl7V3MessageStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Builds an HL7 v3 DOM from a YAML mapping and a semantic Map, the reverse of the reader. The mapping template gives
 * the element of each value; the data gives the value. Each write starts from a fresh DOM, parsed from the template
 * bytes when a template is configured, so an instance can be shared by several threads.
 */
public final class Hl7V3MapToXmlTransformer {

    private static final String HL7_V3_URI = Hl7V3Namespaces.HL7_V3_URI;
    private static final String HL7_PREFIX = "hl7";
    private static final String ROOT_KEY = "raiz";
    private static final String ROOT_KEY_ALIAS = "root";
    private static final String FORMAT_KEY = "formato";
    private static final String FORMAT_KEY_ALIAS = "format";
    private static final String INVALID_ROOT_NAME = "(?s)(?![\\p{L}_][\\p{L}\\p{N}_.\\-]*\\z).*";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
    private static final DateTimeFormatter OFFSET_DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ", Locale.ROOT);

    private final Map<String, Object> template;
    private final byte[] templateBytes;
    private final String rootName;
    private final Map<String, String> coordinates;
    private final Hl7V3Namespaces namespaces;

    /**
     * Creates a transformer and validates every writable XPath of the mapping.
     *
     * @param mapping the mapping, already copied and validated for reading; the template is taken from it
     * @param templateBytes the template XML, or {@code null} to build the root from the {@code raiz} key
     * @param namespaces the namespace declarations of the mapping and of the builder
     * @param registry the strategy registry, used to find the strategy of the root element
     * @throws Hl7MappingException if the root cannot be determined, the template is not HL7 v3, or a path is not writable
     */
    public Hl7V3MapToXmlTransformer(Map<String, Object> mapping, byte[] templateBytes, Hl7V3Namespaces namespaces,
                                    Hl7V3StrategyRegistry registry) {
        this.template = Hl7V3ToMapTransformer.templateOf(mapping);
        this.namespaces = namespaces;
        this.templateBytes = templateBytes == null ? null : templateBytes.clone();
        Hl7V3Strategy strategy;
        if (this.templateBytes != null) {
            Element templateRoot = parseTemplate(this.templateBytes).getDocumentElement();
            if (!HL7_V3_URI.equals(templateRoot.getNamespaceURI())) {
                throw new Hl7MappingException("Writer template root '" + templateRoot.getLocalName()
                    + "' is not in namespace " + HL7_V3_URI);
            }
            this.rootName = templateRoot.getLocalName();
            strategy = registry.find(templateRoot).orElse(new Hl7V3MessageStrategy());
        } else {
            this.rootName = rootNameOf(mapping);
            Element probe = Hl7V3Xml.newDocument().createElementNS(HL7_V3_URI, rootName);
            strategy = registry.find(probe).orElse(strategyForFormat(mapping));
        }
        this.coordinates = strategy.semanticCoordinates();
        Hl7V3WriterValidator.validate(template, namespaces, strategy);
    }

    /**
     * Builds the DOM of one document from the data. The data may be {@code null}: only constants are then written.
     *
     * @param data the semantic Map, keyed as the mapping template
     * @return a new document whose root element is the template root or the {@code raiz} element
     * @throws Hl7MappingException if a value does not fit its node, such as a map where a list is expected
     */
    public Document transform(Map<String, Object> data) {
        Document dom;
        Element root;
        if (templateBytes != null) {
            dom = parseTemplate(templateBytes);
            root = dom.getDocumentElement();
        } else {
            dom = Hl7V3Xml.newDocument();
            root = dom.createElementNS(HL7_V3_URI, rootName);
            dom.appendChild(root);
        }
        applyTemplate(root, root, data, template, "");
        return dom;
    }

    private void applyTemplate(Element root, Element context, Map<?, ?> data, Map<?, ?> templateMap, String path) {
        for (Map.Entry<?, ?> entry : templateMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String childPath = path.isEmpty() ? key : path + "." + key;
            Object value = data == null ? null : data.get(key);
            applyNode(root, context, value, entry.getValue(), childPath);
        }
    }

    private void applyNode(Element root, Element context, Object value, Object node, String path) {
        if (node instanceof String) {
            writeValue(root, context, (String) node, value, null, path);
            return;
        }
        if (!(node instanceof Map<?, ?>)) {
            return;
        }
        Map<?, ?> map = (Map<?, ?>) node;
        Hl7V3DirectiveKind kind = Hl7V3DirectiveKind.classify(map);
        if (kind == Hl7V3DirectiveKind.LIST) {
            applyList(root, context, value, map, path);
        } else if (kind == Hl7V3DirectiveKind.CONSTANT) {
            applyConstant(root, context, map, path);
        } else if (kind == Hl7V3DirectiveKind.COORDINATE) {
            applyCoordinate(root, context, value, map, path);
        } else {
            if (value != null && !(value instanceof Map<?, ?>)) {
                throw new Hl7MappingException("Expected a map for the group at '" + path + "'");
            }
            applyTemplate(root, context, (Map<?, ?>) value, map, path);
        }
    }

    private void applyList(Element root, Element context, Object value, Map<?, ?> map, String path) {
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?>)) {
            throw new Hl7MappingException("Expected a list for lista at '" + path + "'");
        }
        Object spec = map.containsKey("lista") ? map.get("lista") : map.get("list");
        Hl7V3WritablePath target = Hl7V3WritablePath.parse(resolve(String.valueOf(spec)));
        Element start = isCoordinate(String.valueOf(spec)) ? root : context;
        List<Hl7V3WritablePath.Step> steps = target.steps();
        Hl7V3WritablePath.Step last = steps.get(steps.size() - 1);
        Map<?, ?> itemTemplate = (Map<?, ?>) map.get("item");
        Element parent = null;
        for (Object item : (List<?>) value) {
            if (item != null && !(item instanceof Map<?, ?>)) {
                throw new Hl7MappingException("Expected a map for each item of lista at '" + path + "'");
            }
            if (parent == null) {
                parent = resolveSteps(start, steps.subList(0, steps.size() - 1));
            }
            Element created = createChild(parent, namespaceOf(last.prefix()), last);
            applyTemplate(root, created, (Map<?, ?>) item, itemTemplate, path + ".item");
        }
    }

    private void applyConstant(Element root, Element context, Map<?, ?> map, String path) {
        Object xpath = xpathOf(map);
        if (xpath == null) {
            return;
        }
        Object constant = map.containsKey("valor") ? map.get("valor") : map.get("value");
        writeValue(root, context, String.valueOf(xpath), constant, typeNameOf(map), path);
    }

    private void applyCoordinate(Element root, Element context, Object value, Map<?, ?> map, String path) {
        Object effective = value;
        if (effective == null) {
            effective = map.get("padrao");
        }
        writeValue(root, context, String.valueOf(xpathOf(map)), effective, typeNameOf(map), path);
    }

    private void writeValue(Element root, Element context, String expression, Object value, String typeName, String path) {
        if (value == null) {
            return;
        }
        String text = formatValue(value, typeName, path);
        Element start = isCoordinate(expression) ? root : context;
        Hl7V3WritablePath target = Hl7V3WritablePath.parse(resolve(expression));
        Element current = resolveSteps(start, target.steps());
        if (target.attributeQName() != null) {
            String qualified = target.attributeQName();
            String prefix = Hl7V3WritablePath.prefixOf(qualified);
            current.setAttributeNS(prefix.isEmpty() ? null : namespaces.getNamespaceURI(prefix), qualified, text);
        } else {
            current.setTextContent(text);
        }
    }

    private Element resolveSteps(Element start, List<Hl7V3WritablePath.Step> steps) {
        Element current = start;
        for (Hl7V3WritablePath.Step step : steps) {
            current = resolveStep(current, step);
        }
        return current;
    }

    private Element resolveStep(Element parent, Hl7V3WritablePath.Step step) {
        String ns = namespaceOf(step.prefix());
        List<Element> matches = matchingChildren(parent, ns, step);
        if (step.position() > 0) {
            while (matches.size() < step.position()) {
                matches.add(createChild(parent, ns, step));
            }
            return matches.get(step.position() - 1);
        }
        if (!matches.isEmpty()) {
            return matches.get(0);
        }
        return createChild(parent, ns, step);
    }

    private List<Element> matchingChildren(Element parent, String ns, Hl7V3WritablePath.Step step) {
        List<Element> matches = new ArrayList<>();
        Node child = parent.getFirstChild();
        while (child != null) {
            if (child instanceof Element && isMatch((Element) child, ns, step)) {
                matches.add((Element) child);
            }
            child = child.getNextSibling();
        }
        return matches;
    }

    private boolean isMatch(Element candidate, String ns, Hl7V3WritablePath.Step step) {
        if (!Objects.equals(ns, candidate.getNamespaceURI())
                || !step.localName().equals(candidate.getLocalName())) {
            return false;
        }
        for (Hl7V3WritablePath.Predicate predicate : step.predicates()) {
            String qualified = predicate.attributeQName();
            String attributeNs = namespaceOfAttribute(qualified);
            String local = Hl7V3WritablePath.localNameOf(qualified);
            if (!candidate.hasAttributeNS(attributeNs, local)
                    || !predicate.value().equals(candidate.getAttributeNS(attributeNs, local))) {
                return false;
            }
        }
        return true;
    }

    private Element createChild(Element parent, String ns, Hl7V3WritablePath.Step step) {
        Document owner = parent.getOwnerDocument();
        String name = HL7_PREFIX.equals(step.prefix()) ? step.localName() : step.qualifiedName();
        Element child = owner.createElementNS(ns, name);
        for (Hl7V3WritablePath.Predicate predicate : step.predicates()) {
            String qualified = predicate.attributeQName();
            child.setAttributeNS(namespaceOfAttribute(qualified), qualified, predicate.value());
        }
        parent.appendChild(child);
        return child;
    }

    private String namespaceOf(String prefix) {
        if (prefix.isEmpty()) {
            return null;
        }
        if (HL7_PREFIX.equals(prefix)) {
            return HL7_V3_URI;
        }
        return namespaces.getNamespaceURI(prefix);
    }

    private String namespaceOfAttribute(String qualified) {
        String prefix = Hl7V3WritablePath.prefixOf(qualified);
        if (prefix.isEmpty()) {
            return null;
        }
        return namespaceOf(prefix);
    }

    private String resolve(String expression) {
        String trimmed = expression.trim();
        if (!trimmed.startsWith("#")) {
            return trimmed;
        }
        return coordinates.get(trimmed.substring(1).trim());
    }

    private static boolean isCoordinate(String expression) {
        return expression.trim().startsWith("#");
    }

    private static Object xpathOf(Map<?, ?> map) {
        return map.containsKey("xpath") ? map.get("xpath") : map.get("path");
    }

    private static String typeNameOf(Map<?, ?> map) {
        Object type = map.containsKey("tipo") ? map.get("tipo") : map.get("type");
        return type == null ? null : String.valueOf(type);
    }

    private static String formatValue(Object value, String typeName, String path) {
        if (value instanceof Map<?, ?> || value instanceof Collection<?>) {
            throw new Hl7MappingException("Value at '" + path + "' must be a scalar, not a map or list");
        }
        if (value instanceof LocalDate) {
            return DATE_FORMAT.format((LocalDate) value);
        }
        if (value instanceof LocalDateTime) {
            return DATE_TIME_FORMAT.format((LocalDateTime) value);
        }
        if (value instanceof OffsetDateTime) {
            return OFFSET_DATE_TIME_FORMAT.format((OffsetDateTime) value);
        }
        if (value instanceof ZonedDateTime) {
            return OFFSET_DATE_TIME_FORMAT.format(((ZonedDateTime) value).toOffsetDateTime());
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof String && isDateType(typeName)) {
            return isoToTimestamp((String) value);
        }
        return String.valueOf(value);
    }

    private static boolean isDateType(String typeName) {
        if (typeName == null) {
            return false;
        }
        String clean = typeName.trim().toLowerCase(Locale.ROOT);
        return "data".equals(clean) || "date".equals(clean) || "datetime".equals(clean);
    }

    private static String isoToTimestamp(String text) {
        String trimmed = text.trim();
        try {
            return OFFSET_DATE_TIME_FORMAT.format(OffsetDateTime.parse(trimmed));
        } catch (DateTimeParseException ignored) {
            // Not an ISO offset date-time: the local forms are tried next.
        }
        try {
            return DATE_TIME_FORMAT.format(LocalDateTime.parse(trimmed));
        } catch (DateTimeParseException ignored) {
            // Not an ISO local date-time: a date-only value is tried next.
        }
        try {
            return DATE_FORMAT.format(LocalDate.parse(trimmed));
        } catch (DateTimeParseException ignored) {
            // Not an ISO date either: the text is written as it is.
        }
        return text;
    }

    private static String rootNameOf(Map<String, Object> mapping) {
        Object raw = mapping.containsKey(ROOT_KEY) ? mapping.get(ROOT_KEY) : mapping.get(ROOT_KEY_ALIAS);
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            throw new Hl7MappingException("HL7 v3 writer needs a template or a 'raiz' in the mapping");
        }
        String name = String.valueOf(raw).trim();
        if (name.matches(INVALID_ROOT_NAME)) {
            throw new Hl7MappingException("Invalid 'raiz' value '" + name + "' at 'raiz': expected an element local name");
        }
        return name;
    }

    private static Hl7V3Strategy strategyForFormat(Map<String, Object> mapping) {
        Object declared = mapping.containsKey(FORMAT_KEY) ? mapping.get(FORMAT_KEY) : mapping.get(FORMAT_KEY_ALIAS);
        Hl7V3Format format = declared == null ? null : Hl7V3Format.fromName(String.valueOf(declared));
        if (format == Hl7V3Format.CDA) {
            return new CdaR2Strategy();
        }
        return new Hl7V3MessageStrategy();
    }

    private static Document parseTemplate(byte[] bytes) {
        try {
            DocumentBuilder builder = Hl7V3Xml.newSecureFactory().newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // Warnings do not change the template; only errors make it unusable.
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
            return builder.parse(new ByteArrayInputStream(bytes));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot configure the secure XML parser", e);
        } catch (SAXException e) {
            throw new Hl7MappingException("Invalid HL7 v3 writer template: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new Hl7MappingException("Error reading the HL7 v3 writer template", e);
        }
    }
}
