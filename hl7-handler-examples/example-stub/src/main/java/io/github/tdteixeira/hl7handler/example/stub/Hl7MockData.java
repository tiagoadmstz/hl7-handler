package io.github.tdteixeira.hl7handler.example.stub;

import io.github.tdteixeira.hl7handler.Hl7Handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Mock data provider containing simulated HL7 messages for hospital admissions,
 * lab results, and patient transfers.
 */
public final class Hl7MockData {

    private Hl7MockData() {}

    /**
     * Standard ADT_A01 (Patient Admission) HL7 v2.5 message.
     */
    public static final String ADT_A01_V25 =
            "MSH|^~\\&|SIS_HOSPITAL|HOSP_CENTRAL|LAB_SYSTEM|LAB_CENTRAL|20261109123000||ADT^A01^ADT_A01|MSG-2026-001|P|2.5\r" +
            "EVN|A01|20261109123000\r" +
            "PID|1||987654^^^HOSPITAL^MR||ALMEIDA^CAROLINA^SANTOS^^DRA||19920310|F|||AV PAULISTA 1000^APTO 42^SAO PAULO^SP^01310-100^BRA||(11)98765-4321|||C\r" +
            "PV1|1|I|UTI^02^A^^^HOSPITAL||||4567^SILVA^ROBERTO^^^CRM\r";

    /**
     * Standard ADT_A01 HL7 v2.3 message (legacy format).
     */
    public static final String ADT_A01_V23 =
            "MSH|^~\\&|LEGACY_APP|HOSP_SUL|PORTAL|CENTRAL|20261109110000||ADT^A01|MSG-LEGACY-99|P|2.3\r" +
            "PID|1||112233||FERREIRA^MARCOS||19781120|M|||RUA XV DE NOVEMBRO 500^^CURITIBA^PR^80020-310||(41)99111-2233\r" +
            "PV1|1|O\r";

    /**
     * Standard ORU_R01 (Observational Report / Lab Result) HL7 v2.5 message.
     */
    public static final String ORU_R01_LAB =
            "MSH|^~\\&|LAB_LIS|LAB_LOCAL|EPIC_HIS|HOSPITAL|20261109130000||ORU^R01^ORU_R01|LAB-2026-9876|P|2.5\r" +
            "PID|1||987654^^^HOSPITAL||ALMEIDA^CAROLINA\r" +
            "OBR|1|REQ1001|LIS2002|HEM^HEMOGRAMA COMPLETO^LN|||20261109120000\r" +
            "OBX|1|NM|GLU^GLICOSE JEJUM^LN||92|mg/dL|70-99|N|||F\r" +
            "OBX|2|NM|COL^COLESTEROL TOTAL^LN||185|mg/dL|<200|N|||F\r";

    /**
     * Returns the sample message wrapped into standard MLLP network frame (<VT> ... <FS><CR>).
     */
    public static byte[] asMllpFrame(String hl7Message) {
        return Hl7Handler.network().wrap(hl7Message);
    }

    /**
     * Returns an InputStream simulating an active network socket sending an MLLP message.
     */
    public static InputStream asMllpStream(String hl7Message) {
        return new ByteArrayInputStream(asMllpFrame(hl7Message));
    }
}
