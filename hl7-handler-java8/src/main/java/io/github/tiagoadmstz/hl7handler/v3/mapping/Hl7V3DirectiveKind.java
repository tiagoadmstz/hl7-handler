package io.github.tiagoadmstz.hl7handler.v3.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Classifies a YAML mapping node of an HL7 v3 mapping as a directive or as a nested group. A map is a directive
 * only when every one of its keys belongs to that directive's key set, so a business group such as
 * {@code {valor: ..., unidade: ...}} stays a group. The reader, the validator and the XML writer share these rules.
 */
public enum Hl7V3DirectiveKind {

    LIST,
    CONSTANT,
    COORDINATE,
    GROUP;

    private static final Set<String> LIST_KEYS = keySet("lista", "list", "item");
    private static final Set<String> CONSTANT_KEYS = keySet("valor", "value", "tipo", "type", "xpath", "path");
    private static final Set<String> COORDINATE_KEYS = keySet("xpath", "path", "fallback", "padrao", "tipo", "type");

    /**
     * Classifies a mapping node. A constant may also carry an {@code xpath}: the writer uses it as the place where the
     * constant is written, while the reader always returns the constant itself.
     *
     * @param node the YAML node, a map
     * @return the directive kind, or {@link #GROUP} for a nested group
     */
    public static Hl7V3DirectiveKind classify(Map<?, ?> node) {
        if (isDirective(node, LIST_KEYS, "lista", "list")) {
            return LIST;
        }
        if (isDirective(node, CONSTANT_KEYS, "valor", "value")) {
            return CONSTANT;
        }
        if (isDirective(node, COORDINATE_KEYS, "xpath", "path")) {
            return COORDINATE;
        }
        return GROUP;
    }

    private static boolean isDirective(Map<?, ?> node, Set<String> allowedKeys, String... requiredKeys) {
        for (Object key : node.keySet()) {
            if (!allowedKeys.contains(String.valueOf(key))) {
                return false;
            }
        }
        for (String requiredKey : requiredKeys) {
            if (node.containsKey(requiredKey)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> keySet(String... keys) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(keys)));
    }
}
