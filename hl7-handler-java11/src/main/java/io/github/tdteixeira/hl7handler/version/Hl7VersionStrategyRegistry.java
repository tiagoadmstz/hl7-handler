package io.github.tdteixeira.hl7handler.version;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.v21.Hl7V21Strategy;
import io.github.tdteixeira.hl7handler.version.v22.Hl7V22Strategy;
import io.github.tdteixeira.hl7handler.version.v23.Hl7V231Strategy;
import io.github.tdteixeira.hl7handler.version.v23.Hl7V23Strategy;
import io.github.tdteixeira.hl7handler.version.v24.Hl7V24Strategy;
import io.github.tdteixeira.hl7handler.version.v25.Hl7V251Strategy;
import io.github.tdteixeira.hl7handler.version.v25.Hl7V25Strategy;
import io.github.tdteixeira.hl7handler.version.v26.Hl7V26Strategy;
import io.github.tdteixeira.hl7handler.version.v27.Hl7V271Strategy;
import io.github.tdteixeira.hl7handler.version.v27.Hl7V27Strategy;
import io.github.tdteixeira.hl7handler.version.v28.Hl7V28Strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Thread-safe registry and selector of HL7 version strategies.
 */
public final class Hl7VersionStrategyRegistry {

    private static final Hl7VersionStrategy BASELINE_STRATEGY = new Hl7V25Strategy();
    private static final ConcurrentNavigableMap<Hl7Version, Hl7VersionStrategy> STRATEGIES = initStrategies();

    private Hl7VersionStrategyRegistry() {
    }

    private static ConcurrentNavigableMap<Hl7Version, Hl7VersionStrategy> initStrategies() {
        ConcurrentNavigableMap<Hl7Version, Hl7VersionStrategy> map = new ConcurrentSkipListMap<>();
        register(map, new Hl7V21Strategy());
        register(map, new Hl7V22Strategy());
        register(map, new Hl7V23Strategy());
        register(map, new Hl7V231Strategy());
        register(map, new Hl7V24Strategy());
        register(map, new Hl7V25Strategy());
        register(map, new Hl7V251Strategy());
        register(map, new Hl7V26Strategy());
        register(map, new Hl7V27Strategy());
        register(map, new Hl7V271Strategy());
        register(map, new Hl7V28Strategy());
        return map;
    }

    private static void register(Map<Hl7Version, Hl7VersionStrategy> map, Hl7VersionStrategy strategy) {
        map.put(strategy.getVersion(), strategy);
    }

    /**
     * Finds the strategy matching the specified version.
     * 1. Exact match.
     * 2. Nearest known lower version (floor), e.g. 2.9 falls back to 2.8.
     * 3. Baseline strategy (v2.5) if version is unknown or null.
     *
     * @param version HL7 version
     * @return the matching or fallback version strategy
     */
    public static Hl7VersionStrategy findStrategy(Hl7Version version) {
        if (version == null || version.isUnknown()) {
            return BASELINE_STRATEGY;
        }

        Hl7VersionStrategy exact = STRATEGIES.get(version);
        if (exact != null) {
            return exact;
        }

        Map.Entry<Hl7Version, Hl7VersionStrategy> floor = STRATEGIES.floorEntry(version);
        if (floor != null) {
            return floor.getValue();
        }

        return BASELINE_STRATEGY;
    }

    /**
     * Registers a custom strategy.
     *
     * @param strategy strategy to register
     */
    public static void registerStrategy(Hl7VersionStrategy strategy) {
        if (strategy != null && strategy.getVersion() != null) {
            STRATEGIES.put(strategy.getVersion(), strategy);
        }
    }

    /**
     * Returns the baseline default strategy (v2.5).
     *
     * @return baseline strategy
     */
    public static Hl7VersionStrategy getBaselineStrategy() {
        return BASELINE_STRATEGY;
    }

    /**
     * Returns an unmodifiable list of registered strategies.
     *
     * @return list of registered strategies
     */
    public static List<Hl7VersionStrategy> getRegisteredStrategies() {
        return Collections.unmodifiableList(new ArrayList<>(STRATEGIES.values()));
    }
}
