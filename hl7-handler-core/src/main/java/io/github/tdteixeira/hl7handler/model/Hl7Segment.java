package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single line/segment in an HL7 message (e.g. MSH, PID, PV1).
 */
public class Hl7Segment {

    private final String name;
    private final List<Hl7Field> fields;

    public Hl7Segment(String name) {
        this.name = name != null ? name.trim().toUpperCase() : "";
        this.fields = new ArrayList<>();
    }

    public Hl7Segment(String name, List<Hl7Field> fields) {
        this.name = name != null ? name.trim().toUpperCase() : "";
        this.fields = fields != null ? new ArrayList<>(fields) : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<Hl7Field> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public void addField(Hl7Field field) {
        this.fields.add(field != null ? field : new Hl7Field());
    }

    /**
     * Retrieves an HL7 field by 1-based index.
     * E.g. getField(3) on PID returns PID-3.
     */
    public Hl7Field getField(int field1Based) {
        int idx = field1Based - 1;
        if (idx >= 0 && idx < fields.size()) {
            return fields.get(idx);
        }
        return new Hl7Field();
    }

    /**
     * Helper to get string value of a field.
     */
    public String getFieldValue(int field1Based) {
        return getField(field1Based).getValue();
    }

    /**
     * Helper to get specific component of a field (1-based).
     */
    public String getComponentValue(int field1Based, int component1Based) {
        return getField(field1Based).getComponent(component1Based);
    }

    @Override
    public String toString() {
        return name + " (" + fields.size() + " fields)";
    }
}
