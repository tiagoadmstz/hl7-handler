package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an HL7 Field which can have repetitions and components.
 */
public class Hl7Field {

    private final List<List<Hl7Component>> repetitions;

    public Hl7Field() {
        this.repetitions = new ArrayList<>();
    }

    public Hl7Field(List<List<Hl7Component>> repetitions) {
        this.repetitions = repetitions != null ? new ArrayList<>(repetitions) : new ArrayList<>();
    }

    public void addRepetition(List<Hl7Component> components) {
        this.repetitions.add(components != null ? new ArrayList<>(components) : new ArrayList<>());
    }

    public List<List<Hl7Component>> getRepetitions() {
        return Collections.unmodifiableList(repetitions);
    }

    /**
     * Gets components of the first repetition.
     */
    public List<Hl7Component> getComponents() {
        return getComponents(1);
    }

    /**
     * Gets components for a specific 1-based repetition index.
     */
    public List<Hl7Component> getComponents(int repetition1Based) {
        int idx = repetition1Based - 1;
        if (idx >= 0 && idx < repetitions.size()) {
            return Collections.unmodifiableList(repetitions.get(idx));
        }
        return Collections.emptyList();
    }

    /**
     * Returns the raw or joined value of the first repetition.
     */
    public String getValue() {
        return getValue(1);
    }

    /**
     * Returns value of a specific repetition.
     */
    public String getValue(int repetition1Based) {
        List<Hl7Component> comps = getComponents(repetition1Based);
        if (comps.isEmpty()) {
            return "";
        }
        if (comps.size() == 1) {
            return comps.get(0).getValue();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comps.size(); i++) {
            if (i > 0) sb.append('^');
            sb.append(comps.get(i).getValue());
        }
        return sb.toString();
    }

    /**
     * Retrieves a specific 1-based component value from the first repetition.
     */
    public String getComponent(int component1Based) {
        return getComponent(1, component1Based);
    }

    /**
     * Retrieves a specific 1-based component value from a 1-based repetition.
     */
    public String getComponent(int repetition1Based, int component1Based) {
        List<Hl7Component> comps = getComponents(repetition1Based);
        int idx = component1Based - 1;
        if (idx >= 0 && idx < comps.size()) {
            return comps.get(idx).getValue();
        }
        return "";
    }

    @Override
    public String toString() {
        return getValue();
    }
}
