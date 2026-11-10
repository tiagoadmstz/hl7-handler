package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single component in an HL7 field, which may contain subcomponents.
 */
public class Hl7Component {

    private final String value;
    private final List<String> subcomponents;

    public Hl7Component(String value) {
        this.value = value != null ? value : "";
        this.subcomponents = new ArrayList<>();
    }

    public Hl7Component(String value, List<String> subcomponents) {
        this.value = value != null ? value : "";
        this.subcomponents = subcomponents != null ? new ArrayList<>(subcomponents) : new ArrayList<>();
    }

    public String getValue() {
        return value;
    }

    public List<String> getSubcomponents() {
        return Collections.unmodifiableList(subcomponents);
    }

    public String getSubcomponent(int index1Based) {
        int idx = index1Based - 1;
        if (idx >= 0 && idx < subcomponents.size()) {
            return subcomponents.get(idx);
        }
        return idx == 0 ? value : "";
    }

    @Override
    public String toString() {
        return value;
    }
}
