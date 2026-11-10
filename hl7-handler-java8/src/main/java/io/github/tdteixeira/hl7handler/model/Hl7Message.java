package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Represents a complete parsed HL7 v2 Message.
 */
public class Hl7Message {

    private final Hl7Version version;
    private final Hl7Delimiters delimiters;
    private final List<Hl7Segment> segments;

    public Hl7Message(Hl7Version version, Hl7Delimiters delimiters, List<Hl7Segment> segments) {
        this.version = version != null ? version : Hl7Version.UNKNOWN;
        this.delimiters = delimiters != null ? delimiters : Hl7Delimiters.DEFAULT;
        this.segments = segments != null ? new ArrayList<Hl7Segment>(segments) : new ArrayList<Hl7Segment>();
    }

    public Hl7Version getVersion() {
        return version;
    }

    public Hl7Delimiters getDelimiters() {
        return delimiters;
    }

    public List<Hl7Segment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * Finds the first segment matching the given name (case-insensitive).
     */
    public Optional<Hl7Segment> getSegment(String segmentName) {
        if (segmentName == null) {
            return Optional.empty();
        }
        for (Hl7Segment seg : segments) {
            if (seg.getName().equalsIgnoreCase(segmentName.trim())) {
                return Optional.of(seg);
            }
        }
        return Optional.empty();
    }

    /**
     * Finds all segments matching the given name.
     */
    public List<Hl7Segment> getSegments(String segmentName) {
        if (segmentName == null) {
            return Collections.emptyList();
        }
        List<Hl7Segment> matched = new ArrayList<Hl7Segment>();
        for (Hl7Segment seg : segments) {
            if (seg.getName().equalsIgnoreCase(segmentName.trim())) {
                matched.add(seg);
            }
        }
        return matched;
    }

    /**
     * Resolves a coordinate path like "PID.5.1", "PID-5.1", "PID.3[2].1", "OBX[2].5" or "PID".
     */
    public String get(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        String normalized = path.trim().replace('-', '.');
        String[] parts = normalized.split("\\.");
        if (parts.length == 0) {
            return "";
        }

        String segPart = parts[0];
        int segmentIndex = 1;

        if (segPart.contains("[") && segPart.endsWith("]")) {
            int openIdx = segPart.indexOf('[');
            String idxStr = segPart.substring(openIdx + 1, segPart.length() - 1);
            segPart = segPart.substring(0, openIdx);
            try {
                segmentIndex = Math.max(1, Integer.parseInt(idxStr));
            } catch (NumberFormatException ignored) {
                return "";
            }
        }

        List<Hl7Segment> matched = getSegments(segPart);
        if (segmentIndex < 1 || segmentIndex > matched.size()) {
            return "";
        }
        Hl7Segment seg = matched.get(segmentIndex - 1);

        if (parts.length == 1) {
            return seg.toRawString(delimiters);
        }

        try {
            String fieldPart = parts[1];
            int fieldIndex;
            int repIndex = 1;

            if (fieldPart.contains("[") && fieldPart.endsWith("]")) {
                int open = fieldPart.indexOf('[');
                repIndex = Math.max(1, Integer.parseInt(fieldPart.substring(open + 1, fieldPart.length() - 1)));
                fieldIndex = Integer.parseInt(fieldPart.substring(0, open));
            } else {
                fieldIndex = Integer.parseInt(fieldPart);
            }

            if (parts.length == 2) {
                return seg.getFieldValue(fieldIndex, repIndex);
            }

            int componentIndex = Integer.parseInt(parts[2]);
            if (parts.length == 3) {
                return seg.getComponentValue(fieldIndex, repIndex, componentIndex);
            }

            int subcomponentIndex = Integer.parseInt(parts[3]);
            Hl7Field field = seg.getField(fieldIndex);
            List<Hl7Component> comps = field.getComponents(repIndex);
            int cIdx = componentIndex - 1;
            if (cIdx >= 0 && cIdx < comps.size()) {
                return comps.get(cIdx).getSubcomponent(subcomponentIndex);
            }
        } catch (Exception ignored) {
            return "";
        }

        return "";
    }
}
