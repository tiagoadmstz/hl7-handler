package io.github.tiagoadmstz.hl7handler.v3.version.cda;

import io.github.tiagoadmstz.hl7handler.v3.Hl7V3Document;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy for HL7 C-CDA documents, which are CDA R2 documents that carry the US Realm Header template
 * {@code 2.16.840.1.113883.10.20.22.1.1}. The version comes from that template's extension: {@code C-CDA-R2.1} for
 * 2015-08-01, {@code C-CDA-R2.0} for 2014-06-09, {@code C-CDA-R1.1} when there is no extension, and {@code C-CDA}
 * for any other value.
 */
public final class CcdaStrategy extends CdaR2Strategy {

    private static final String US_REALM_HEADER = "2.16.840.1.113883.10.20.22.1.1";
    private static final String TEMPLATE_ID = "templateId";
    private static final String LOINC = "2.16.840.1.113883.6.1";
    private static final String REALM_HEADER_XPATH = "hl7:templateId[@root='" + US_REALM_HEADER + "']/@extension";
    private static final String EXTENSION_R21 = "2015-08-01";
    private static final String EXTENSION_R20 = "2014-06-09";

    /**
     * Creates the C-CDA strategy with the CDA coordinates plus the C-CDA ones.
     */
    public CcdaStrategy() {
        super(coordinates());
    }

    @Override
    public boolean supports(Element root) {
        return "ClinicalDocument".equals(root.getLocalName()) && hasRealmHeader(root);
    }

    @Override
    public String versionIdentifier(Hl7V3Document document) {
        String extension = document.get(REALM_HEADER_XPATH);
        if (extension.isEmpty()) {
            return "C-CDA-R1.1";
        }
        if (EXTENSION_R21.equals(extension)) {
            return "C-CDA-R2.1";
        }
        if (EXTENSION_R20.equals(extension)) {
            return "C-CDA-R2.0";
        }
        return "C-CDA";
    }

    @Override
    public List<String> validate(Hl7V3Document document) {
        List<String> warnings = new ArrayList<>(super.validate(document));
        if (!LOINC.equals(document.get("hl7:code/@codeSystem"))) {
            warnings.add("C-CDA code/@codeSystem is not LOINC (2.16.840.1.113883.6.1)");
        }
        if (document.getAll("hl7:recordTarget").isEmpty()) {
            warnings.add("C-CDA document has no recordTarget");
        }
        if (document.getAll("hl7:languageCode").isEmpty()) {
            warnings.add("C-CDA document has no languageCode");
        }
        return Collections.unmodifiableList(warnings);
    }

    private static boolean hasRealmHeader(Element root) {
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element && isRealmTemplateId((Element) child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRealmTemplateId(Element element) {
        return TEMPLATE_ID.equals(element.getLocalName())
            && Hl7V3Namespaces.HL7_V3_URI.equals(element.getNamespaceURI())
            && US_REALM_HEADER.equals(element.getAttribute("root"));
    }

    private static Map<String, String> coordinates() {
        Map<String, String> coordinates = new LinkedHashMap<>();
        coordinates.put("documentTemplateId", "hl7:templateId[@root!='" + US_REALM_HEADER + "'][1]/@root");
        coordinates.put("patientGiven", "hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:given[1]");
        coordinates.put("patientFamily", "hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:family");
        coordinates.put("patientBirthTime", "hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:birthTime/@value");
        coordinates.put("patientGender",
            "hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:administrativeGenderCode/@code");
        coordinates.put("authorTime", "hl7:author[1]/hl7:time/@value");
        coordinates.put("custodianName",
            "hl7:custodian/hl7:assignedCustodian/hl7:representedCustodianOrganization/hl7:name");
        return coordinates;
    }
}
