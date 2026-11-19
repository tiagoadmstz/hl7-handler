package io.github.tiagoadmstz.hl7handler.v3.parser;

import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

/**
 * Secure XML factories and serialization shared by the HL7 v3 parser, writer and ACK generator. Factories are never
 * shared: DOM and transformer factories are not thread-safe, so every call creates a new one.
 */
public final class Hl7V3Xml {

    private static final String INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
    private static final String INDENT_SPACES = "2";

    private Hl7V3Xml() {}

    /**
     * Creates a namespace-aware DOM factory with XXE, external entities, DTDs and XInclude disabled.
     *
     * @return a new factory
     * @throws IllegalStateException if the JAXP implementation rejects a hardening feature
     */
    public static DocumentBuilderFactory newSecureFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot configure the secure XML parser", e);
        }
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    /**
     * Creates an empty, namespace-aware DOM document built by the secure factory.
     *
     * @return a new empty document
     * @throws IllegalStateException if the DOM builder cannot be created
     */
    public static Document newDocument() {
        try {
            return newSecureFactory().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot create an XML document", e);
        }
    }

    /**
     * Serializes a DOM as UTF-8 XML with an XML declaration. The output has no DOCTYPE and no external stylesheet
     * access, and the transformer is created for this call only.
     *
     * @param dom the document to serialize
     * @param indent whether to indent child elements by two spaces
     * @return the XML text, declaring UTF-8
     * @throws IllegalStateException if the transformer cannot be created or the DOM cannot be serialized
     */
    public static String toXml(Document dom, boolean indent) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
            if (indent) {
                transformer.setOutputProperty(INDENT_AMOUNT, INDENT_SPACES);
            }
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(dom), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new IllegalStateException("Cannot serialize the XML document", e);
        }
    }
}
