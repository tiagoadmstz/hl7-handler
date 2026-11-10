package io.github.tdteixeira.hl7handler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single component in an HL7 field, which may contain subcomponents.
 * Retains both decoded and raw (escaped) forms.
 */
public class Hl7Component {

    private final String value;
    private final String rawValue;
    private final List<String> subcomponents;
    private final List<String> rawSubcomponents;

    public Hl7Component(String value) {
        this(value, value, Collections.emptyList(), Collections.emptyList());
    }

    public Hl7Component(String value, List<String> subcomponents) {
        this(value, value, subcomponents, subcomponents);
    }

    public Hl7Component(String value, String rawValue, List<String> subcomponents, List<String> rawSubcomponents) {
        this.value = value != null ? value : "";
        this.rawValue = rawValue != null ? rawValue : this.value;
        this.subcomponents = subcomponents != null ? new ArrayList<>(subcomponents) : new ArrayList<>();
        this.rawSubcomponents = rawSubcomponents != null ? new ArrayList<>(rawSubcomponents) : new ArrayList<>();
    }

    public String getValue() {
        return value;
    }

    public String getRawValue() {
        return rawValue;
    }

    public List<String> getSubcomponents() {
        return Collections.unmodifiableList(subcomponents);
    }

    public List<String> getRawSubcomponents() {
        return Collections.unmodifiableList(rawSubcomponents);
    }

    public String getSubcomponent(int index1Based) {
        int idx = index1Based - 1;
        if (idx >= 0 && idx < subcomponents.size()) {
            return subcomponents.get(idx);
        }
        return idx == 0 ? value : "";
    }

    public String getRawSubcomponent(int index1Based) {
        int idx = index1Based - 1;
        if (idx >= 0 && idx < rawSubcomponents.size()) {
            return rawSubcomponents.get(idx);
        }
        return idx == 0 ? rawValue : "";
    }

    @Override
    public String toString() {
        return value;
    }
}
