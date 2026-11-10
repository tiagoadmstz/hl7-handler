package io.github.tdteixeira.hl7handler.writer;

import io.github.tdteixeira.hl7handler.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes HL7 domain models back into standard HL7 v2 text streams.
 */
public class Hl7Writer {

    private final Hl7Delimiters delimiters;
    private final String segmentTerminator;

    public Hl7Writer() {
        this(Hl7Delimiters.DEFAULT, "\r");
    }

    public Hl7Writer(Hl7Delimiters delimiters, String segmentTerminator) {
        this.delimiters = delimiters != null ? delimiters : Hl7Delimiters.DEFAULT;
        this.segmentTerminator = segmentTerminator != null ? segmentTerminator : "\r";
    }

    /**
     * Serializes an Hl7Message into HL7 v2 text representation.
     */
    public String write(Hl7Message message) {
        if (message == null || message.getSegments().isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Hl7Segment segment : message.getSegments()) {
            sb.append(writeSegment(segment, delimiters));
            sb.append(segmentTerminator);
        }

        return sb.toString();
    }

    private String writeSegment(Hl7Segment segment, Hl7Delimiters delims) {
        StringBuilder sb = new StringBuilder(segment.getName());
        boolean isMsh = "MSH".equalsIgnoreCase(segment.getName());

        List<Hl7Field> fields = segment.getFields();
        if (isMsh) {
            // MSH format: MSH|^~\&|field3|field4...
            // Field 1 is the separator itself
            sb.append(delims.fieldSeparator());
            // Field 2 is encoding characters
            if (fields.size() > 1) {
                sb.append(fields.get(1).getValue());
            } else {
                sb.append(delims.componentSeparator())
                  .append(delims.repetitionSeparator())
                  .append(delims.escapeCharacter())
                  .append(delims.subcomponentSeparator());
            }

            for (int i = 2; i < fields.size(); i++) {
                sb.append(delims.fieldSeparator());
                sb.append(fields.get(i).getValue());
            }
        } else {
            for (Hl7Field field : fields) {
                sb.append(delims.fieldSeparator());
                sb.append(field.getValue());
            }
        }

        return sb.toString();
    }
}
