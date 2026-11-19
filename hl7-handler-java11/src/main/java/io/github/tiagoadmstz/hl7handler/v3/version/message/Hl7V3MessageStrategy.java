package io.github.tiagoadmstz.hl7handler.v3.version.message;

import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.version.AbstractHl7V3Strategy;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Strategy for HL7 v3 interaction messages. A root is recognised by an {@code hl7:interactionId} child or by a
 * root name in the interaction pattern, such as {@code PRPA_IN201301UV02}. The version comes from
 * {@code versionCode/@code}, then from {@code @ITSVersion}, and falls back to {@code V3}.
 */
public class Hl7V3MessageStrategy extends AbstractHl7V3Strategy {

    private static final Pattern INTERACTION_NAME = Pattern.compile("[A-Z]{4}_IN\\d{6}[A-Z]{2}\\d{2}");
    private static final String INTERACTION_ID = "interactionId";
    private static final String DEFAULT_VERSION = "V3";
    private static final String ITS_VERSION_XML = "XML_1.0";

    /**
     * Creates the message strategy with its built-in semantic coordinates.
     */
    public Hl7V3MessageStrategy() {
        super(Hl7V3Format.MESSAGE, coordinates());
    }

    @Override
    public boolean supports(Element root) {
        String name = root.getLocalName();
        if (name != null && INTERACTION_NAME.matcher(name).matches()) {
            return true;
        }
        return hasInteractionIdChild(root);
    }

    @Override
    public String versionIdentifier(Hl7V3Document document) {
        String versionCode = document.get("hl7:versionCode/@code");
        if (!versionCode.isEmpty()) {
            return versionCode;
        }
        String itsVersion = document.get("@ITSVersion");
        if (!itsVersion.isEmpty()) {
            return itsVersion;
        }
        return DEFAULT_VERSION;
    }

    @Override
    public List<String> validate(Hl7V3Document document) {
        List<String> warnings = new ArrayList<>();
        if (document.getAll("hl7:" + INTERACTION_ID).isEmpty()) {
            warnings.add("HL7 v3 message has no interactionId");
        }
        String itsVersion = document.get("@ITSVersion");
        if (!itsVersion.isEmpty() && !ITS_VERSION_XML.equals(itsVersion)) {
            warnings.add("ITSVersion '" + itsVersion + "' is not " + ITS_VERSION_XML);
        }
        return Collections.unmodifiableList(warnings);
    }

    private static boolean hasInteractionIdChild(Element root) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element
                    && INTERACTION_ID.equals(child.getLocalName())
                    && Hl7V3Namespaces.HL7_V3_URI.equals(child.getNamespaceURI())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> coordinates() {
        Map<String, String> coordinates = new LinkedHashMap<>();
        coordinates.put("messageId", "hl7:id/@extension");
        coordinates.put("interactionId", "hl7:interactionId/@extension");
        coordinates.put("creationTime", "hl7:creationTime/@value");
        coordinates.put("patientId", "(.//hl7:patient/hl7:id/@extension)[1]");
        coordinates.put("sender", "hl7:sender/hl7:device/hl7:id/@root");
        return coordinates;
    }
}
