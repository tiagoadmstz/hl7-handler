package io.github.tdteixeira.hl7handler.version.v23;

import io.github.tdteixeira.hl7handler.model.Hl7Version;

/**
 * HL7 v2.3.1 strategy: MSH-9 is "ACK^EVENT^ACK", patient ID in PID.3.1.
 */
public class Hl7V231Strategy extends Hl7V23Strategy {

    public Hl7V231Strategy() {
        this(Hl7Version.V2_3_1);
    }

    protected Hl7V231Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public String formatMsh9(String eventTrigger, char componentSeparator) {
        String event = (eventTrigger != null && !eventTrigger.isEmpty()) ? eventTrigger : "R01";
        return "ACK" + componentSeparator + event + componentSeparator + "ACK";
    }

    @Override
    public String getDefaultPatientIdCoordinate() {
        return "PID.3.1";
    }
}
