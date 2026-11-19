package io.github.tiagoadmstz.hl7handler.v3.ack;

import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Generates an HL7 v3 acknowledgement, an MCCI_IN000002UV01 message, for a received HL7 v3 message. The sender and
 * receiver of the acknowledgement are swapped from the received message, and the acknowledged message id is copied
 * from it. The result is serialized by the same secure XML code as the reader and the writer.
 */
public final class Hl7V3AckGenerator {

    private static final String HL7_V3_URI = Hl7V3Namespaces.HL7_V3_URI;
    private static final String ACK_ROOT = "MCCI_IN000002UV01";
    private static final String ACK_INTERACTION_ROOT = "2.16.840.1.113883.1.6";
    private static final String ACK_INTERACTION_EXTENSION = "MCCI_IN000002UV01";
    private static final String DEFAULT_PROCESSING_CODE = "P";
    private static final String PROCESSING_MODE_CODE = "T";
    private static final String ACCEPT_ACK_CODE = "NE";
    private static final String ERROR_DETAIL = "E";
    private static final String INFO_DETAIL = "I";
    private static final DateTimeFormatter CREATION_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssZ", Locale.ROOT);

    private Hl7V3AckGenerator() {}

    /**
     * Generates an application accept (AA) acknowledgement with no text.
     *
     * @param document the received HL7 v3 message
     * @return the acknowledgement XML, declaring UTF-8
     * @throws IllegalArgumentException if the document is {@code null} or is a CDA document
     */
    public static String generateAck(Hl7V3Document document) {
        return generateAck(document, Hl7V3AckCode.AA, null);
    }

    /**
     * Generates an acknowledgement with a code and an optional text. A {@code null} or blank text adds no
     * acknowledgement detail; otherwise the detail is typed as an error for the error codes and as information for
     * AA and CA.
     *
     * @param document the received HL7 v3 message
     * @param code the acknowledgement code
     * @param text the text of the acknowledgement detail, or {@code null}
     * @return the acknowledgement XML, declaring UTF-8
     * @throws IllegalArgumentException if the document or the code is {@code null}, or the document is a CDA document
     */
    public static String generateAck(Hl7V3Document document, Hl7V3AckCode code, String text) {
        if (document == null) {
            throw new IllegalArgumentException("Document must not be null");
        }
        if (code == null) {
            throw new IllegalArgumentException("ACK code must not be null");
        }
        if (document.format() == Hl7V3Format.CDA) {
            throw new IllegalArgumentException("ACK applies only to HL7 v3 messages");
        }
        String detailText = text == null ? "" : text.trim();
        Document dom = Hl7V3Xml.newDocument();
        Element ack = dom.createElementNS(HL7_V3_URI, ACK_ROOT);
        dom.appendChild(ack);
        ack.setAttributeNS(null, "ITSVersion", "XML_1.0");

        Element id = child(ack, "id");
        attribute(id, "root", UUID.randomUUID().toString());
        attribute(child(ack, "creationTime"), "value", CREATION_TIME.format(OffsetDateTime.now()));
        Element interaction = child(ack, "interactionId");
        attribute(interaction, "root", ACK_INTERACTION_ROOT);
        attribute(interaction, "extension", ACK_INTERACTION_EXTENSION);

        String processingCode = document.get("hl7:processingCode/@code");
        attribute(child(ack, "processingCode"), "code",
            processingCode.isEmpty() ? DEFAULT_PROCESSING_CODE : processingCode);
        attribute(child(ack, "processingModeCode"), "code", PROCESSING_MODE_CODE);
        attribute(child(ack, "acceptAckCode"), "code", ACCEPT_ACK_CODE);

        Element receiver = child(ack, "receiver");
        attribute(receiver, "typeCode", "RCV");
        appendDevice(receiver, document, "hl7:sender/hl7:device/hl7:id");

        Element sender = child(ack, "sender");
        attribute(sender, "typeCode", "SND");
        appendDevice(sender, document, "hl7:receiver/hl7:device/hl7:id");

        Element acknowledgement = child(ack, "acknowledgement");
        attribute(acknowledgement, "typeCode", code.name());
        Element target = child(acknowledgement, "targetMessage");
        appendIds(target, document, "hl7:id");
        if (!detailText.isEmpty()) {
            boolean error = code == Hl7V3AckCode.AE || code == Hl7V3AckCode.AR
                || code == Hl7V3AckCode.CE || code == Hl7V3AckCode.CR;
            Element detail = child(acknowledgement, "acknowledgementDetail");
            attribute(detail, "typeCode", error ? ERROR_DETAIL : INFO_DETAIL);
            child(detail, "text").setTextContent(detailText);
        }
        return Hl7V3Xml.toXml(dom, true);
    }

    private static void appendDevice(Element parent, Hl7V3Document document, String idExpression) {
        Element device = child(parent, "device");
        attribute(device, "classCode", "DEV");
        attribute(device, "determinerCode", "INSTANCE");
        appendIds(device, document, idExpression);
    }

    private static void appendIds(Element parent, Hl7V3Document document, String idExpression) {
        List<Node> nodes = document.selectNodes(idExpression, null);
        for (Node node : nodes) {
            String root = document.selectText("@root", node);
            String extension = document.selectText("@extension", node);
            Element id = child(parent, "id");
            if (!root.isEmpty()) {
                attribute(id, "root", root);
            }
            if (!extension.isEmpty()) {
                attribute(id, "extension", extension);
            }
        }
    }

    private static Element child(Element parent, String name) {
        Element element = parent.getOwnerDocument().createElementNS(HL7_V3_URI, name);
        parent.appendChild(element);
        return element;
    }

    private static void attribute(Element element, String name, String value) {
        element.setAttributeNS(null, name, value);
    }
}
