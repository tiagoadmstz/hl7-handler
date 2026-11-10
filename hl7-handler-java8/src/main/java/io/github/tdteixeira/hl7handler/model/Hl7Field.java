package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an HL7 Field which can have repetitions and components.
 */
public class Hl7Field {

    private final char componentSeparator;
    private final List<List<Hl7Component>> repetitions;

    public Hl7Field() {
        this('^', new ArrayList<List<Hl7Component>>());
    }

    public Hl7Field(List<List<Hl7Component>> repetitions) {
        this('^', repetitions);
    }

    public Hl7Field(char componentSeparator, List<List<Hl7Component>> repetitions) {
        this.componentSeparator = componentSeparator;
        this.repetitions = repetitions != null ? new ArrayList<List<Hl7Component>>(repetitions) : new ArrayList<List<Hl7Component>>();
    }

    public char getComponentSeparator() {
        return componentSeparator;
    }

    public void addRepetition(List<Hl7Component> components) {
        this.repetitions.add(components != null ? new ArrayList<Hl7Component>(components) : new ArrayList<Hl7Component>());
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
     * Returns the joined value of the first repetition.
     */
    public String getValue() {
        return getValue(1);
    }

    /**
     * Returns the joined value of a specific 1-based repetition.
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
            if (i > 0) {
                sb.append(componentSeparator);
            }
            sb.append(comps.get(i).getValue());
        }
        return sb.toString();
    }

    /**
     * Returns the raw (escaped) joined value of a specific 1-based repetition.
     */
    public String getRawValue(int repetition1Based) {
        List<Hl7Component> comps = getComponents(repetition1Based);
        if (comps.isEmpty()) {
            return "";
        }
        if (comps.size() == 1) {
            return comps.get(0).getRawValue();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comps.size(); i++) {
            if (i > 0) {
                sb.append(componentSeparator);
            }
            sb.append(comps.get(i).getRawValue());
        }
        return sb.toString();
    }

    /**
     * Returns the decoded values of all repetitions as a list.
     */
    public List<String> getValues() {
        if (repetitions.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<String>(repetitions.size());
        for (int i = 1; i <= repetitions.size(); i++) {
            values.add(getValue(i));
        }
        return Collections.unmodifiableList(values);
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
