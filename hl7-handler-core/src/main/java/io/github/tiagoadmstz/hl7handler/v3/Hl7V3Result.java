package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.binding.Hl7JsonBinder;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.v3.mapping.Hl7V3ToMapTransformer;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of reading one HL7 v3 document. The document is parsed once, when the result is created.
 * The mapped Map is computed on first use, cached, and returned unmodifiable; it is safe to share across threads.
 */
public final class Hl7V3Result {

    private static final String TEXT_KEY = "#text";
    private static final String ATTRIBUTE_PREFIX = "@";

    private final Hl7V3Document document;
    private final Hl7V3ToMapTransformer transformer;
    private final Hl7JsonBinder jsonBinder;
    private final Map<String, Object> mapping;
    private final Object lock = new Object();
    private volatile Map<String, Object> mappedMap;

    Hl7V3Result(
        Hl7V3Document document,
        Hl7V3ToMapTransformer transformer,
        Hl7JsonBinder jsonBinder,
        Map<String, Object> mapping
    ) {
        this.document = document;
        this.transformer = transformer;
        this.jsonBinder = jsonBinder;
        this.mapping = mapping;
    }

    /**
     * Returns the parsed document: its format, version identifier, warnings and XPath access.
     */
    public Hl7V3Document document() {
        return document;
    }

    /**
     * Returns the structural JSON of the document, independent of any YAML mapping. Attributes are keyed as
     * {@code @name}, text as {@code #text}, and repeated sibling elements become a list.
     */
    public String toRawJson() {
        Map<String, Object> raw;
        synchronized (document.lock()) {
            raw = rawRoot(document.root());
        }
        return jsonBinder.toJson(raw);
    }

    /**
     * Returns the mapped semantic Map, computed once and cached. The map and its nested maps and lists are unmodifiable.
     *
     * @throws Hl7MappingException if no YAML mapping was configured
     */
    public Map<String, Object> toMap() {
        Map<String, Object> cached = mappedMap;
        if (cached != null) {
            return cached;
        }
        if (mapping == null) {
            throw new Hl7MappingException("No YAML mapping was configured. Call withMapping() before toMap() or use toRawJson()");
        }
        synchronized (lock) {
            if (mappedMap == null) {
                mappedMap = deepCopy(transformer.transform(document, mapping));
            }
            return mappedMap;
        }
    }

    /**
     * Returns the mapped JSON when a YAML mapping was configured, otherwise the raw structural JSON.
     */
    public String toJson() {
        if (mapping != null) {
            return jsonBinder.toJson(toMap());
        }
        return toRawJson();
    }

    /**
     * Binds the mapped Map into the client's Value Object.
     *
     * @throws Hl7MappingException if no YAML mapping was configured
     */
    public <T> T bindTo(Class<T> targetClass) {
        return jsonBinder.bind(toMap(), targetClass);
    }

    /**
     * Copies a mapped structure as an unmodifiable map, nested maps and lists included, with String keys.
     */
    static Map<String, Object> deepCopy(Map<?, ?> source) {
        return copyMap(source);
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return copyMap((Map<?, ?>) value);
        }
        if (value instanceof List<?>) {
            List<Object> copy = new ArrayList<>();
            for (Object item : (List<?>) value) {
                copy.add(copyValue(item));
            }
            return Collections.unmodifiableList(copy);
        }
        return value;
    }

    private static Map<String, Object> rawRoot(Element root) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put(root.getLocalName(), elementValue(root));
        return wrapper;
    }

    private static Object elementValue(Element element) {
        Map<String, Object> map = new LinkedHashMap<>();
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(attribute.getNamespaceURI())) {
                continue;
            }
            map.put(ATTRIBUTE_PREFIX + attribute.getLocalName(), attribute.getValue());
        }
        StringBuilder text = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                addChild(map, ((Element) child).getLocalName(), elementValue((Element) child));
            } else if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                text.append(child.getNodeValue());
            }
        }
        String value = text.toString().trim();
        if (!value.isEmpty()) {
            map.put(TEXT_KEY, value);
        }
        if (map.isEmpty()) {
            return "";
        }
        if (map.size() == 1 && map.containsKey(TEXT_KEY)) {
            return map.get(TEXT_KEY);
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static void addChild(Map<String, Object> map, String name, Object value) {
        if (!map.containsKey(name)) {
            map.put(name, value);
            return;
        }
        Object existing = map.get(name);
        if (existing instanceof List<?>) {
            ((List<Object>) existing).add(value);
            return;
        }
        List<Object> values = new ArrayList<>();
        values.add(existing);
        values.add(value);
        map.put(name, values);
    }
}
