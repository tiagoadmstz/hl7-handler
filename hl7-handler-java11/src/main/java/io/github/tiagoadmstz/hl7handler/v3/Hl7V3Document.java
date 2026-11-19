package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable HL7 v3 document built by the parser. XPath expressions are relative to the root element, and a
 * {@code #name} expression is resolved through the strategy's semantic coordinates. DOM is not thread-safe, so
 * every evaluation runs under an internal lock.
 */
public final class Hl7V3Document {

    private final Document dom;
    private final Element root;
    private final Hl7V3Strategy strategy;
    private final XPath xpath;
    private final Object lock = new Object();
    private final String versionIdentifier;
    private final List<String> templateIds;
    private final List<String> warnings;

    /**
     * Builds the document and computes its version identifier and warnings from the strategy.
     *
     * @param dom the parsed DOM, namespace-aware
     * @param strategy the strategy selected for the root element
     * @param namespaces the namespace context used by the XPath expressions
     * @param parseWarnings warnings already produced by the parser; may be {@code null}
     */
    public Hl7V3Document(Document dom, Hl7V3Strategy strategy, Hl7V3Namespaces namespaces, List<String> parseWarnings) {
        this.dom = dom;
        this.root = dom.getDocumentElement();
        this.strategy = strategy;
        this.xpath = namespaces.newXPath();
        this.templateIds = getAll("hl7:templateId/@root");
        this.versionIdentifier = strategy.versionIdentifier(this);
        List<String> all = new ArrayList<>();
        if (parseWarnings != null) {
            all.addAll(parseWarnings);
        }
        all.addAll(strategy.validate(this));
        this.warnings = Collections.unmodifiableList(all);
    }

    /**
     * Returns the document family, CDA or MESSAGE.
     */
    public Hl7V3Format format() {
        return strategy.format();
    }

    /**
     * Returns the version identifier, such as {@code CDA-R2} or the message's {@code versionCode}.
     */
    public String versionIdentifier() {
        return versionIdentifier;
    }

    /**
     * Returns the local name of the root element, such as {@code ClinicalDocument} or an interaction name.
     */
    public String rootName() {
        return root.getLocalName();
    }

    /**
     * Returns the values of the root-level {@code templateId/@root} elements, in document order.
     */
    public List<String> templateIds() {
        return templateIds;
    }

    /**
     * Returns the strategy that handles this document.
     */
    public Hl7V3Strategy strategy() {
        return strategy;
    }

    /**
     * Returns the warnings from the parser and from the strategy, in that order.
     */
    public List<String> warnings() {
        return warnings;
    }

    /**
     * Returns the trimmed string value of the first node selected by the XPath, relative to the root element.
     * Returns an empty string when the path does not exist or the {@code #name} is unknown.
     *
     * @param xpath an XPath expression, or {@code #name} for a semantic coordinate
     * @throws Hl7MappingException if the XPath is invalid
     */
    public String get(String xpath) {
        return selectText(xpath, null);
    }

    /**
     * Returns the trimmed string value of every node selected by the XPath, relative to the root element.
     * Returns an empty list when nothing is selected or the {@code #name} is unknown.
     *
     * @param xpath an XPath expression that selects nodes, or {@code #name} for a semantic coordinate
     * @return an unmodifiable list of values
     * @throws Hl7MappingException if the XPath is invalid
     */
    public List<String> getAll(String xpath) {
        List<Node> nodes = selectNodes(xpath, null);
        List<String> values = new ArrayList<>(nodes.size());
        synchronized (lock) {
            for (Node node : nodes) {
                values.add(node.getTextContent().trim());
            }
        }
        return Collections.unmodifiableList(values);
    }

    /**
     * Selects nodes with an XPath. A {@code null} context means the root element. A {@code #name} expression is
     * always evaluated from the root element, whatever the context. The DOM is not thread-safe: use the returned nodes
     * only as the context of {@link #selectText(String, Node)} or {@link #selectNodes(String, Node)}, never read or
     * change them directly.
     *
     * @param expression an XPath expression, or {@code #name} for a semantic coordinate
     * @param context the context node, or {@code null} for the root element
     * @return an unmodifiable list of the selected nodes; empty when the {@code #name} is unknown
     * @throws Hl7MappingException if the XPath is invalid
     */
    public List<Node> selectNodes(String expression, Node context) {
        String trimmed = trimmed(expression);
        String resolved = resolve(trimmed);
        if (resolved == null) {
            return Collections.emptyList();
        }
        Node start = startNode(trimmed, context);
        synchronized (lock) {
            try {
                NodeList nodes = (NodeList) xpath.evaluate(resolved, start, XPathConstants.NODESET);
                List<Node> result = new ArrayList<>(nodes.getLength());
                for (int i = 0; i < nodes.getLength(); i++) {
                    result.add(nodes.item(i));
                }
                return Collections.unmodifiableList(result);
            } catch (XPathExpressionException e) {
                throw new Hl7MappingException("Invalid XPath '" + resolved + "'", e);
            }
        }
    }

    /**
     * Returns the trimmed string value of an XPath expression. A {@code null} context means the root element. A
     * {@code #name} expression is always evaluated from the root element, whatever the context.
     *
     * @param expression an XPath expression, or {@code #name} for a semantic coordinate
     * @param context the context node, or {@code null} for the root element
     * @return the value, or an empty string when the path does not exist or the {@code #name} is unknown
     * @throws Hl7MappingException if the XPath is invalid
     */
    public String selectText(String expression, Node context) {
        String trimmed = trimmed(expression);
        String resolved = resolve(trimmed);
        if (resolved == null) {
            return "";
        }
        Node start = startNode(trimmed, context);
        synchronized (lock) {
            try {
                return xpath.evaluate(resolved, start).trim();
            } catch (XPathExpressionException e) {
                throw new Hl7MappingException("Invalid XPath '" + resolved + "'", e);
            }
        }
    }

    Element root() {
        return root;
    }

    Object lock() {
        return lock;
    }

    private String trimmed(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new Hl7MappingException("XPath expression must not be null or blank");
        }
        return expression.trim();
    }

    private String resolve(String trimmed) {
        if (!trimmed.startsWith("#")) {
            return trimmed;
        }
        return strategy.semanticCoordinates().get(trimmed.substring(1));
    }

    private Node startNode(String trimmed, Node context) {
        if (context == null || trimmed.startsWith("#")) {
            return root;
        }
        return context;
    }
}
