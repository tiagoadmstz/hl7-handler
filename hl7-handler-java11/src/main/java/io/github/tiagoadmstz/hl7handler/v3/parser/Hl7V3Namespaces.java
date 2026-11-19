package io.github.tiagoadmstz.hl7handler.v3.parser;

import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Namespace prefixes used by the HL7 v3 XPath expressions. The prefixes {@code hl7}, {@code sdtc} and {@code xsi}
 * are built in; extra prefixes come from the builder or from the {@code namespaces} block of the mapping. The
 * built-in prefixes cannot be bound to another URI, because {@code hl7} must always mean HL7 v3.
 */
public final class Hl7V3Namespaces implements NamespaceContext {

    /** Namespace URI of HL7 v3 elements, such as {@code ClinicalDocument} and {@code interactionId}. */
    public static final String HL7_V3_URI = "urn:hl7-org:v3";

    private static final String SDTC_URI = "urn:hl7-org:sdtc";
    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private final Map<String, String> uriByPrefix;

    /**
     * Creates the context with the built-in prefixes plus the given declarations.
     *
     * @param declared extra prefix to URI declarations; may be {@code null}
     * @throws Hl7MappingException if a prefix or URI is blank, or a built-in prefix is rebound
     */
    public Hl7V3Namespaces(Map<String, String> declared) {
        Map<String, String> prefixes = new LinkedHashMap<>();
        prefixes.put("hl7", HL7_V3_URI);
        prefixes.put("sdtc", SDTC_URI);
        prefixes.put("xsi", XSI_URI);
        if (declared != null) {
            for (Map.Entry<String, String> entry : declared.entrySet()) {
                String prefix = entry.getKey() == null ? "" : entry.getKey().trim();
                String uri = entry.getValue() == null ? "" : entry.getValue().trim();
                if (prefix.isEmpty() || uri.isEmpty()) {
                    throw new Hl7MappingException("Namespace declaration needs a prefix and a URI: '" + entry.getKey() + "'");
                }
                String builtIn = prefixes.get(prefix);
                if (builtIn != null && !builtIn.equals(uri)) {
                    throw new Hl7MappingException("Namespace prefix '" + prefix + "' is reserved for " + builtIn);
                }
                prefixes.put(prefix, uri);
            }
        }
        this.uriByPrefix = Collections.unmodifiableMap(prefixes);
    }

    /**
     * Returns the URI bound to the prefix, or {@link XMLConstants#NULL_NS_URI} when the prefix is unknown.
     */
    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix must not be null");
        }
        String uri = uriByPrefix.get(prefix);
        if (uri == null) {
            return XMLConstants.NULL_NS_URI;
        }
        return uri;
    }

    /**
     * Returns the first prefix bound to the URI, or {@code null} when none is bound.
     */
    @Override
    public String getPrefix(String namespaceURI) {
        Iterator<String> prefixes = getPrefixes(namespaceURI);
        if (prefixes.hasNext()) {
            return prefixes.next();
        }
        return null;
    }

    /**
     * Returns every prefix bound to the URI, as an unmodifiable iterator.
     */
    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Namespace URI must not be null");
        }
        List<String> prefixes = new ArrayList<>();
        for (Map.Entry<String, String> entry : uriByPrefix.entrySet()) {
            if (entry.getValue().equals(namespaceURI)) {
                prefixes.add(entry.getKey());
            }
        }
        return Collections.unmodifiableList(prefixes).iterator();
    }

    /**
     * Creates a new XPath evaluator bound to this namespace context, with secure processing enabled.
     * XPath objects are not thread-safe, so each caller gets its own instance.
     *
     * @return a new XPath bound to this context
     */
    public XPath newXPath() {
        XPathFactory factory = XPathFactory.newInstance();
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (XPathFactoryConfigurationException e) {
            throw new IllegalStateException("Cannot enable secure XPath processing", e);
        }
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(this);
        return xpath;
    }
}
