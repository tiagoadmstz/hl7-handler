package io.github.tdteixeira.hl7handler.version.v22;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v21.Hl7V21Strategy;

/**
 * HL7 v2.2 strategy: MSH-9 is "ACK^EVENT", event in MSH.9.2, no MSH-18.
 */
public class Hl7V22Strategy extends Hl7V21Strategy {

    public Hl7V22Strategy() {
        this(Hl7Version.V2_2);
    }

    protected Hl7V22Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public String formatMsh9(String eventTrigger, char componentSeparator) {
        String event = (eventTrigger != null && !eventTrigger.isEmpty()) ? eventTrigger : "R01";
        return "ACK" + componentSeparator + event;
    }

    @Override
    public String getDefaultEventCoordinate() {
        return "MSH.9.2";
    }
}
