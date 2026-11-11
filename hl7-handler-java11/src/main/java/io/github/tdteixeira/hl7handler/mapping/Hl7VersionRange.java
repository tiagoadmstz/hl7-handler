package io.github.tdteixeira.hl7handler.mapping;

import io.github.tdteixeira.hl7handler.model.Hl7Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable evaluator and parser for HL7 v2 version constraints and ranges.
 * Supports exact versions ("2.5"), comparison operators (">=2.3", "<2.3.1"),
 * compound expressions (">=2.3, <2.7"), wildcards ("2.x", "*") and specificity ranking.
 */
public final class Hl7VersionRange {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
            "([><!=]=?|=)?\\s*([0-9]+(?:\\.[0-9]+)*(?:\\.[xX*])?|[xX*]|any|all)",
            Pattern.CASE_INSENSITIVE
    );

    private final String rawExpression;
    private final List<Condition> conditions;
    private final int specificity;

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        MAJOR_WILDCARD,
        ANY
    }

    public static final class Condition {
        private final Operator operator;
        private final Hl7Version targetVersion;
        private final int majorWildcard;

        public Condition(Operator operator, Hl7Version targetVersion, int majorWildcard) {
            this.operator = operator;
            this.targetVersion = targetVersion;
            this.majorWildcard = majorWildcard;
        }

        public Operator getOperator() {
            return operator;
        }

        public Hl7Version getTargetVersion() {
            return targetVersion;
        }

        public int getMajorWildcard() {
            return majorWildcard;
        }

        public boolean matches(Hl7Version version) {
            if (version == null || version.isUnknown()) {
                return false;
            }

            switch (operator) {
                case EQUALS:
                    return version.compareTo(targetVersion) == 0;
                case NOT_EQUALS:
                    return version.compareTo(targetVersion) != 0;
                case GREATER_THAN:
                    return version.compareTo(targetVersion) > 0;
                case GREATER_THAN_OR_EQUAL:
                    return version.compareTo(targetVersion) >= 0;
                case LESS_THAN:
                    return version.compareTo(targetVersion) < 0;
                case LESS_THAN_OR_EQUAL:
                    return version.compareTo(targetVersion) <= 0;
                case MAJOR_WILDCARD:
                    return version.getMajor() == majorWildcard;
                case ANY:
                    return true;
                default:
                    return false;
            }
        }
    }

    private Hl7VersionRange(String rawExpression, List<Condition> conditions, int specificity) {
        this.rawExpression = rawExpression;
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.specificity = specificity;
    }

    /**
     * Parses a version expression into an Hl7VersionRange.
     *
     * @param expression e.g. ">=2.3", "<2.3.1", "2.1", ">=2.3, <2.7", "2.x", "*"
     * @return the parsed version range
     * @throws Hl7MappingException if the expression syntax is invalid
     */
    public static Hl7VersionRange parse(String expression) {
        if (expression == null || expression.trim().isEmpty() || "*".equals(expression.trim()) || "any".equalsIgnoreCase(expression.trim())) {
            List<Condition> conds = Collections.singletonList(new Condition(Operator.ANY, Hl7Version.UNKNOWN, 0));
            return new Hl7VersionRange(expression != null ? expression.trim() : "*", conds, 0);
        }

        String clean = expression.trim();
        Matcher matcher = TOKEN_PATTERN.matcher(clean);
        List<Condition> conditions = new ArrayList<>();
        int lastEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            if (start > lastEnd) {
                String between = clean.substring(lastEnd, start).trim();
                if (!between.isEmpty() && !between.equals(",")) {
                    throw new Hl7MappingException("Invalid version range expression: '" + expression + "'");
                }
            }
            lastEnd = matcher.end();

            String opStr = matcher.group(1);
            String verStr = matcher.group(2);

            Condition cond = parseCondition(opStr, verStr, expression);
            conditions.add(cond);
        }

        if (lastEnd < clean.length()) {
            String tail = clean.substring(lastEnd).trim();
            if (!tail.isEmpty() && !tail.equals(",")) {
                throw new Hl7MappingException("Invalid version range expression: '" + expression + "'");
            }
        }

        if (conditions.isEmpty()) {
            throw new Hl7MappingException("Invalid version range expression: '" + expression + "'");
        }

        int specificity = calculateSpecificity(conditions);
        return new Hl7VersionRange(clean, conditions, specificity);
    }

    private static Condition parseCondition(String opStr, String verStr, String fullExpr) {
        String cleanVer = verStr.trim();

        if ("*".equals(cleanVer) || "any".equalsIgnoreCase(cleanVer) || "all".equalsIgnoreCase(cleanVer)) {
            return new Condition(Operator.ANY, Hl7Version.UNKNOWN, 0);
        }

        if (cleanVer.toLowerCase().endsWith(".x") || cleanVer.toLowerCase().endsWith(".*")) {
            int dotIdx = cleanVer.indexOf('.');
            String majorStr = cleanVer.substring(0, dotIdx);
            try {
                int maj = Integer.parseInt(majorStr);
                return new Condition(Operator.MAJOR_WILDCARD, Hl7Version.UNKNOWN, maj);
            } catch (NumberFormatException e) {
                throw new Hl7MappingException("Invalid version wildcard in: '" + fullExpr + "'", e);
            }
        }

        Hl7Version version = Hl7Version.fromIdentifier(cleanVer);
        if (version.isUnknown()) {
            throw new Hl7MappingException("Unrecognized HL7 version '" + verStr + "' in: '" + fullExpr + "'");
        }

        Operator operator = determineOperator(opStr);
        return new Condition(operator, version, 0);
    }

    private static Operator determineOperator(String opStr) {
        if (opStr == null || opStr.trim().isEmpty() || "=".equals(opStr.trim()) || "==".equals(opStr.trim())) {
            return Operator.EQUALS;
        }
        String clean = opStr.trim();
        switch (clean) {
            case ">=":
                return Operator.GREATER_THAN_OR_EQUAL;
            case "<=":
                return Operator.LESS_THAN_OR_EQUAL;
            case ">":
                return Operator.GREATER_THAN;
            case "<":
                return Operator.LESS_THAN;
            case "!=":
                return Operator.NOT_EQUALS;
            default:
                return Operator.EQUALS;
        }
    }

    private static int calculateSpecificity(List<Condition> conditions) {
        int score = 0;
        boolean hasLowerBound = false;
        boolean hasUpperBound = false;

        for (Condition cond : conditions) {
            switch (cond.getOperator()) {
                case EQUALS:
                    score += 1000;
                    if (cond.getTargetVersion().getPatch() > 0) {
                        score += 10;
                    }
                    break;
                case NOT_EQUALS:
                    score += 50;
                    break;
                case GREATER_THAN:
                case GREATER_THAN_OR_EQUAL:
                    score += 100;
                    if (cond.getTargetVersion().getPatch() > 0) {
                        score += 5;
                    }
                    hasLowerBound = true;
                    break;
                case LESS_THAN:
                case LESS_THAN_OR_EQUAL:
                    score += 100;
                    if (cond.getTargetVersion().getPatch() > 0) {
                        score += 5;
                    }
                    hasUpperBound = true;
                    break;
                case MAJOR_WILDCARD:
                    score += 10;
                    break;
                case ANY:
                    break;
                default:
                    break;
            }
        }

        if (hasLowerBound && hasUpperBound) {
            score += 400; // Bounded interval
        }

        return score;
    }

    /**
     * Checks if the given Hl7Version satisfies this range constraint.
     */
    public boolean matches(Hl7Version version) {
        if (version == null || version.isUnknown()) {
            return false;
        }
        for (Condition cond : conditions) {
            if (!cond.matches(version)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given string version satisfies this range constraint.
     */
    public boolean matches(String versionIdentifier) {
        if (versionIdentifier == null) {
            return false;
        }
        return matches(Hl7Version.fromIdentifier(versionIdentifier));
    }

    public int getSpecificity() {
        return specificity;
    }

    public String getRawExpression() {
        return rawExpression;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public static Comparator<Hl7VersionRange> specificityComparator() {
        return Comparator.comparingInt(Hl7VersionRange::getSpecificity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hl7VersionRange)) {
            return false;
        }
        Hl7VersionRange that = (Hl7VersionRange) o;
        return Objects.equals(rawExpression, that.rawExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawExpression);
    }

    @Override
    public String toString() {
        return rawExpression;
    }
}
