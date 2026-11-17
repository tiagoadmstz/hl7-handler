package io.github.tiagoadmstz.hl7handler.mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Classifies a YAML mapping node as a directive or as a nested group. A map is a directive only when
 * every one of its keys belongs to that directive's key set, so a business group such as
 * {@code {valor: OBX.5, unidade: OBX.6}} stays a group. Shared by the transformer and the mapping
 * validator so both read the same rules.
 */
enum Hl7DirectiveKind {

    LIST,
    CONSTANT,
    COORDINATE,
    GROUP;

    private static final Set<String> LIST_KEYS = keySet("lista", "list", "item");
    private static final Set<String> CONSTANT_KEYS = keySet("valor", "value", "tipo", "type");
    private static final Set<String> COORDINATE_KEYS = keySet("hl7", "path", "campo", "fallback", "padrao", "tipo", "type");

    static Hl7DirectiveKind classify(Map<?, ?> node) {
        if (isDirective(node, LIST_KEYS, "lista", "list")) {
            return LIST;
        }
        if (isDirective(node, CONSTANT_KEYS, "valor", "value")) {
            return CONSTANT;
        }
        if (isDirective(node, COORDINATE_KEYS, "hl7", "path", "campo")) {
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
