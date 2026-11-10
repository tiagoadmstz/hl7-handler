package io.github.tdteixeira.hl7handler.writer;

import io.github.tdteixeira.hl7handler.model.Hl7Component;
import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Escapes;
import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;

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
     * Serializes an Hl7Message into HL7 v2 text representation, using the message's delimiters.
     */
    public String write(Hl7Message message) {
        if (message == null || message.getSegments().isEmpty()) {
            return "";
        }

        Hl7Delimiters delims = message.getDelimiters() != null ? message.getDelimiters() : this.delimiters;
        StringBuilder sb = new StringBuilder();
        for (Hl7Segment segment : message.getSegments()) {
            sb.append(formatSegment(segment, delims));
            sb.append(segmentTerminator);
        }

        return sb.toString();
    }

    /**
     * Formats a single segment into raw HL7 text representation.
     */
    public static String formatSegment(Hl7Segment segment, Hl7Delimiters delims) {
        if (segment == null) {
            return "";
        }

        Hl7Delimiters d = delims != null ? delims : Hl7Delimiters.DEFAULT;
        StringBuilder sb = new StringBuilder(segment.getName());
        boolean isMsh = "MSH".equalsIgnoreCase(segment.getName());

        List<Hl7Field> fields = segment.getFields();
        if (isMsh) {
            sb.append(d.fieldSeparator());
            if (fields.size() > 1) {
                String msh2 = fields.get(1).getRawValue(1);
                if (msh2.isEmpty()) {
                    msh2 = fields.get(1).getValue();
                }
                sb.append(msh2);
            } else {
                sb.append(d.componentSeparator())
                  .append(d.repetitionSeparator())
                  .append(d.escapeCharacter())
                  .append(d.subcomponentSeparator());
            }

            for (int i = 2; i < fields.size(); i++) {
                sb.append(d.fieldSeparator());
                sb.append(formatField(fields.get(i), d));
            }
        } else {
            for (Hl7Field field : fields) {
                sb.append(d.fieldSeparator());
                sb.append(formatField(field, d));
            }
        }

        return sb.toString();
    }

    /**
     * Formats an Hl7Field with all its repetitions, components, and subcomponents.
     */
    public static String formatField(Hl7Field field, Hl7Delimiters delims) {
        if (field == null) {
            return "";
        }

        List<List<Hl7Component>> repetitions = field.getRepetitions();
        if (repetitions.isEmpty()) {
            return "";
        }

        Hl7Delimiters d = delims != null ? delims : Hl7Delimiters.DEFAULT;
        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < repetitions.size(); r++) {
            if (r > 0) {
                sb.append(d.repetitionSeparator());
            }

            List<Hl7Component> components = repetitions.get(r);
            for (int c = 0; c < components.size(); c++) {
                if (c > 0) {
                    sb.append(d.componentSeparator());
                }

                Hl7Component comp = components.get(c);
                List<String> rawSubs = comp.getRawSubcomponents();
                if (!rawSubs.isEmpty()) {
                    for (int s = 0; s < rawSubs.size(); s++) {
                        if (s > 0) {
                            sb.append(d.subcomponentSeparator());
                        }
                        sb.append(rawSubs.get(s));
                    }
                } else if (comp.getRawValue() != null && !comp.getRawValue().isEmpty()) {
                    sb.append(comp.getRawValue());
                } else {
                    sb.append(Hl7Escapes.encode(comp.getValue(), d));
                }
            }
        }

        return sb.toString();
    }
}
