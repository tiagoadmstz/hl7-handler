package io.github.tiagoadmstz.hl7handler.v3.version.cda;

import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Format;
import io.github.tiagoadmstz.hl7handler.v3.version.AbstractHl7V3Strategy;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for HL7 CDA R2 clinical documents, whose root element is {@code ClinicalDocument}.
 * The {@code documentId} coordinate reads the first {@code id}: its {@code @extension}, or its {@code @root}
 * when the extension is empty.
 */
public class CdaR2Strategy extends AbstractHl7V3Strategy {

    private static final String VERSION_IDENTIFIER = "CDA-R2";
    private static final String TYPE_ID_ROOT = "2.16.840.1.113883.1.3";
    private static final String TYPE_ID_EXTENSION = "POCD_HD000040";

    /**
     * Creates the CDA R2 strategy with its built-in semantic coordinates.
     */
    public CdaR2Strategy() {
        super(Hl7V3Format.CDA, coordinates());
    }

    @Override
    public boolean supports(Element root) {
        return "ClinicalDocument".equals(root.getLocalName());
    }

    @Override
    public String versionIdentifier(Hl7V3Document document) {
        return VERSION_IDENTIFIER;
    }

    @Override
    public List<String> validate(Hl7V3Document document) {
        String typeIdRoot = document.get("hl7:typeId/@root");
        String typeIdExtension = document.get("hl7:typeId/@extension");
        if (TYPE_ID_ROOT.equals(typeIdRoot) && TYPE_ID_EXTENSION.equals(typeIdExtension)) {
            return Collections.emptyList();
        }
        return Collections.singletonList("CDA typeId is not 2.16.840.1.113883.1.3/POCD_HD000040");
    }

    private static Map<String, String> coordinates() {
        Map<String, String> coordinates = new LinkedHashMap<>();
        coordinates.put("documentId", "concat(hl7:id[1]/@extension, hl7:id[1][string-length(@extension) = 0]/@root)");
        coordinates.put("patientId", "hl7:recordTarget/hl7:patientRole/hl7:id[1]/@extension");
        coordinates.put("effectiveTime", "hl7:effectiveTime/@value");
        coordinates.put("documentType", "hl7:code/@code");
        coordinates.put("title", "hl7:title");
        return coordinates;
    }
}
