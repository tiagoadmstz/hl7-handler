package io.github.tdteixeira.hl7handler.example.stub;

import module java.base;

import io.github.tdteixeira.hl7handler.Hl7Handler;

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
     * ORU_R01 HL7 v2.5 lab result with six OBX segments (fictitious data), read as a list.
     */
    public static final String ORU_R01_HEMOGRAMA_V25 =
            "MSH|^~\\&|LAB_LIS|LAB_LOCAL|EPIC_HIS|HOSPITAL|20261117090000||ORU^R01^ORU_R01|LAB-2026-5001|P|2.5\r" +
            "PID|1||555001^^^HOSPITAL^MR||PACIENTE^TESTE FICTICIO\r" +
            "OBR|1|REQ5001|LIS5001|HMG^HEMOGRAMA^LN|||20261117080000\r" +
            "OBX|1|NM|HGB^HEMOGLOBINA^LN||13.8|g/dL|12.0-16.0|N|||F\r" +
            "OBX|2|NM|HCT^HEMATOCRITO^LN||41.2|%|36.0-46.0|N|||F\r" +
            "OBX|3|NM|WBC^LEUCOCITOS^LN||11.5|mil/uL|4.0-11.0|H|||F\r" +
            "OBX|4|NM|PLT^PLAQUETAS^LN||245|mil/uL|150-450|N|||F\r" +
            "OBX|5|NM|MCV^VOLUME CORPUSCULAR MEDIO^LN||88|fL|80-100|N|||F\r" +
            "OBX|6|NM|RDW^AMPLITUDE DE DISTRIBUICAO DOS ERITROCITOS^LN||13.1|%|11.5-14.5|N|||F\r";

    /**
     * ORU_R01 HL7 v2.6 vital signs from a multiparameter monitor (fictitious data).
     * MSH-18 is UNICODE UTF-8, MSH-3 has components and timestamps are DTM with offset.
     */
    public static final String ORU_R01_MONITOR_V26 =
            "MSH|^~\\&|MONITOR^DEV-FICT-0001^EUI64|UTI_LEITO_12|PRONTUARIO^HOSP_FICT|HOSP_FICT|20261117090012.1200-0300||ORU^R01^ORU_R01|MON-2026-7788|P|2.6||||||UNICODE UTF-8\r" +
            "PID|1||555002^^^HOSP_FICT^MR||PACIENTE^FICTICIO DOIS||19800101|F\r" +
            "PV1|1|I|UTI^12^A\r" +
            "OBR|1||MON-2026-7788|VITAL^SINAIS VITAIS^MDC|||20261117090000.0000-0300\r" +
            "OBX|1|NM|150456^HR^MDC||72|/min|60-100|N|||F|||20261117090010.0000-0300\r" +
            "OBX|2|NM|150452^SPO2^MDC||97|%|95-100|N|||F|||20261117090010.0000-0300\r" +
            "OBX|3|NM|150369^BP_SYS^MDC||121|mmHg|90-140|N|||F|||20261117090010.0000-0300\r" +
            "OBX|4|NM|150370^BP_DIA^MDC||78|mmHg|60-90|N|||F|||20261117090010.0000-0300\r" +
            "OBX|5|NM|150376^RR^MDC||16|/min|12-20|N|||F|||20261117090010.0000-0300\r" +
            "OBX|6|NM|150322^TEMP^MDC||36.7|Cel|35.5-37.5|N|||F|||20261117090010.0000-0300\r";

    /**
     * ADT_A01 HL7 v2.3 admission (fictitious data) with the patient identifier in PID-2 and PID-3 empty.
     */
    public static final String ADT_A01_V23_PID2 =
            "MSH|^~\\&|LEGADO_APP|HOSP_FICT|PORTAL|CENTRAL|20261117100000||ADT^A01|LEG-2026-3001|P|2.3\r" +
            "PID|1|PAC-7001|||PEREIRA^ANA FICTICIA||19900515|F\r" +
            "PV1|1|I|CLI^03^B\r";

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
