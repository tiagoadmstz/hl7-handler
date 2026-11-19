package io.github.tiagoadmstz.hl7handler.v3.parser;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import io.github.tiagoadmstz.hl7handler.v3.version.message.Hl7V3MessageStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Parses HL7 v3 XML into an {@link Hl7V3Document}. The DOM factory is hardened against XXE and external
 * resources, and a new factory and builder are created for each parse because they are not thread-safe.
 */
public final class Hl7V3Parser {

    private final Hl7V3StrategyRegistry registry;
    private final Hl7V3Namespaces namespaces;

    /**
     * Creates a parser that selects strategies from the registry and evaluates XPath with the namespaces.
     *
     * @param registry the strategy registry
     * @param namespaces the namespace context for XPath
     */
    public Hl7V3Parser(Hl7V3StrategyRegistry registry, Hl7V3Namespaces namespaces) {
        this.registry = registry;
        this.namespaces = namespaces;
    }

    /**
     * Parses the XML source. The XML parser detects the encoding from the declaration when the source is a byte stream.
     *
     * @param source the XML source
     * @return the parsed document
     * @throws Hl7ParseException if the XML is malformed, contains a DOCTYPE, or its root is not in the HL7 v3 namespace
     */
    public Hl7V3Document parse(InputSource source) {
        Document dom = parseDom(source);
        Element root = dom.getDocumentElement();
        if (!Hl7V3Namespaces.HL7_V3_URI.equals(root.getNamespaceURI())) {
            throw new Hl7ParseException("Not an HL7 v3 document: root element '" + root.getLocalName()
                + "' is not in namespace " + Hl7V3Namespaces.HL7_V3_URI);
        }
        Optional<Hl7V3Strategy> found = registry.find(root);
        if (found.isPresent()) {
            return new Hl7V3Document(dom, found.get(), namespaces, Collections.<String>emptyList());
        }
        String warning = "No HL7 v3 strategy supports root element '" + root.getLocalName() + "'; using MESSAGE fallback";
        return new Hl7V3Document(dom, new Hl7V3MessageStrategy(), namespaces, Collections.singletonList(warning));
    }

    private static Document parseDom(InputSource source) {
        try {
            DocumentBuilder builder = newSecureFactory().newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // Warnings do not make the document invalid; the default handler would only print them to stderr.
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
            return builder.parse(source);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Cannot configure the secure XML parser", e);
        } catch (SAXException e) {
            throw new Hl7ParseException("Malformed HL7 v3 XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new Hl7ParseException("Error reading HL7 v3 XML", e);
        }
    }

    private static DocumentBuilderFactory newSecureFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
