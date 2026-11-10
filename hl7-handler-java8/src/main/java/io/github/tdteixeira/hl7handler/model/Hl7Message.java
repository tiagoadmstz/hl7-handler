package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        this.segments = segments != null ? new ArrayList<>(segments) : new ArrayList<>();
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
        if (segmentName == null) return Optional.empty();
        return segments.stream()
                .filter(s -> s.getName().equalsIgnoreCase(segmentName.trim()))
                .findFirst();
    }

    /**
     * Finds all segments matching the given name.
     */
    public List<Hl7Segment> getSegments(String segmentName) {
        if (segmentName == null) return Collections.emptyList();
        return segments.stream()
                .filter(s -> s.getName().equalsIgnoreCase(segmentName.trim()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a coordinate path like "PID.5.1" or "MSH.9" or "OBX[1].5".
     */
    public String get(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "";
        }

        String[] parts = path.trim().split("\\.");
        if (parts.length == 0) return "";

        String segPart = parts[0];
        int segmentIndex = 0;

        // Check for indexed segment like OBX[1] (1-based index)
        if (segPart.contains("[") && segPart.endsWith("]")) {
            int openIdx = segPart.indexOf('[');
            String idxStr = segPart.substring(openIdx + 1, segPart.length() - 1);
            segPart = segPart.substring(0, openIdx);
            try {
                segmentIndex = Math.max(0, Integer.parseInt(idxStr) - 1);
            } catch (NumberFormatException ignored) {}
        }

        List<Hl7Segment> matched = getSegments(segPart);
        if (segmentIndex >= matched.size()) {
            return "";
        }
        Hl7Segment seg = matched.get(segmentIndex);

        if (parts.length == 1) {
            return seg.toString();
        }

        try {
            int fieldIndex = Integer.parseInt(parts[1]);
            if (parts.length == 2) {
                return seg.getFieldValue(fieldIndex);
            }

            int componentIndex = Integer.parseInt(parts[2]);
            if (parts.length == 3) {
                return seg.getComponentValue(fieldIndex, componentIndex);
            }

            int subcomponentIndex = Integer.parseInt(parts[3]);
            Hl7Field field = seg.getField(fieldIndex);
            List<Hl7Component> comps = field.getComponents();
            int cIdx = componentIndex - 1;
            if (cIdx >= 0 && cIdx < comps.size()) {
                return comps.get(cIdx).getSubcomponent(subcomponentIndex);
            }
        } catch (NumberFormatException ignored) {
            return "";
        }

        return "";
    }
}
