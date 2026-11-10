package io.github.tdteixeira.hl7handler.version.v26;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v25.Hl7V251Strategy;

import java.time.OffsetDateTime;

/**
 * HL7 v2.6 strategy: DTM date format with microseconds and explicit timezone.
 */
public class Hl7V26Strategy extends Hl7V251Strategy {

    public Hl7V26Strategy() {
        this(Hl7Version.V2_6);
    }

    protected Hl7V26Strategy(Hl7Version version) {
        super(version);
    }

    @Override
    public String formatHeaderTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DTM_FORMATTER);
    }
}
