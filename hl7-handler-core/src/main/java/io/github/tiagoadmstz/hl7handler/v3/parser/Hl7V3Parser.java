package io.github.tiagoadmstz.hl7handler.v3.parser;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3SchemaMode;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3SchemaValidationException;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import io.github.tiagoadmstz.hl7handler.v3.version.message.Hl7V3MessageStrategy;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses HL7 v3 XML into an {@link Hl7V3Document}. The DOM factory is hardened against XXE and external
 * resources, and a new factory and builder are created for each parse because they are not thread-safe. When a
 * schema is given, the instance is validated against it; its own {@code xsi:schemaLocation} hints are not loaded.
 */
public final class Hl7V3Parser {

    private static final String EXTERNAL_SCHEMA_LOCATION = "http://apache.org/xml/properties/schema/external-schemaLocation";
    private static final String EXTERNAL_NO_NAMESPACE_SCHEMA_LOCATION =
        "http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation";

    private final Hl7V3StrategyRegistry registry;
    private final Hl7V3Namespaces namespaces;
    private final Schema schema;
    private final Hl7V3SchemaMode schemaMode;

    /**
     * Creates a parser that selects strategies from the registry and evaluates XPath with the namespaces. The
     * parser validates no schema.
     *
     * @param registry the strategy registry
     * @param namespaces the namespace context for XPath
     */
    public Hl7V3Parser(Hl7V3StrategyRegistry registry, Hl7V3Namespaces namespaces) {
        this(registry, namespaces, null, Hl7V3SchemaMode.STRICT);
    }

    /**
     * Creates a parser that also validates each document against a compiled XSD schema.
     *
     * @param registry the strategy registry
     * @param namespaces the namespace context for XPath
     * @param schema the compiled schema, or {@code null} to skip XSD validation
     * @param schemaMode how a violation is reported; {@code null} means {@link Hl7V3SchemaMode#STRICT}
     */
    public Hl7V3Parser(Hl7V3StrategyRegistry registry, Hl7V3Namespaces namespaces, Schema schema, Hl7V3SchemaMode schemaMode) {
        this.registry = registry;
        this.namespaces = namespaces;
        this.schema = schema;
        this.schemaMode = schemaMode == null ? Hl7V3SchemaMode.STRICT : schemaMode;
    }

    /**
     * Parses the XML source. The XML parser detects the encoding from the declaration when the source is a byte stream.
     *
     * @param source the XML source
     * @return the parsed document
     * @throws Hl7ParseException if the XML is malformed, contains a DOCTYPE, or its root is not in the HL7 v3 namespace
     * @throws Hl7V3SchemaValidationException in STRICT mode, when the document does not match the schema
     */
    public Hl7V3Document parse(InputSource source) {
        List<String> schemaErrors = new ArrayList<>();
        Document dom = parseDom(source, schemaErrors);
        Element root = dom.getDocumentElement();
        if (!Hl7V3Namespaces.HL7_V3_URI.equals(root.getNamespaceURI())) {
            throw new Hl7ParseException("Not an HL7 v3 document: root element '" + root.getLocalName()
                + "' is not in namespace " + Hl7V3Namespaces.HL7_V3_URI);
        }
        if (!schemaErrors.isEmpty() && schemaMode == Hl7V3SchemaMode.STRICT) {
            throw new Hl7V3SchemaValidationException(
                "HL7 v3 document does not match the XSD schema: " + schemaErrors.get(0), schemaErrors);
        }
        List<String> warnings = new ArrayList<>();
        if (schemaMode == Hl7V3SchemaMode.WARN) {
            warnings.addAll(schemaErrors);
        }
        Optional<Hl7V3Strategy> found = registry.find(root);
        if (found.isPresent()) {
            return new Hl7V3Document(dom, found.get(), namespaces, warnings);
        }
        warnings.add("No HL7 v3 strategy supports root element '" + root.getLocalName() + "'; using MESSAGE fallback");
        return new Hl7V3Document(dom, new Hl7V3MessageStrategy(), namespaces, warnings);
    }

    private Document parseDom(InputSource source, final List<String> schemaErrors) {
        try {
            DocumentBuilderFactory factory = Hl7V3Xml.newSecureFactory();
            if (schema != null) {
                factory.setSchema(schema);
                factory.setAttribute(EXTERNAL_SCHEMA_LOCATION, "");
                factory.setAttribute(EXTERNAL_NO_NAMESPACE_SCHEMA_LOCATION, "");
            }
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                    // Warnings do not make the document invalid; the default handler would only print them to stderr.
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    if (schema == null) {
                        throw exception;
                    }
                    schemaErrors.add(formatSchemaError(exception));
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

    private static String formatSchemaError(SAXParseException exception) {
        return "XSD line " + exception.getLineNumber() + ", column " + exception.getColumnNumber() + ": "
            + exception.getMessage();
    }
}
