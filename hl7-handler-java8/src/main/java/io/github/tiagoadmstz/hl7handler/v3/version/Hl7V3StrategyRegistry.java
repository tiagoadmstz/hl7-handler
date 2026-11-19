package io.github.tiagoadmstz.hl7handler.v3.version;

import io.github.tiagoadmstz.hl7handler.v3.version.cda.CcdaStrategy;
import io.github.tiagoadmstz.hl7handler.v3.version.cda.CdaR2Strategy;
import io.github.tiagoadmstz.hl7handler.v3.version.message.Hl7V3MessageStrategy;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe, instance-based registry of HL7 v3 strategies. Custom strategies registered with
 * {@link #register(Hl7V3Strategy)} are placed before the built-in ones, newest first, so they win the lookup.
 */
public final class Hl7V3StrategyRegistry {

    private final List<Hl7V3Strategy> strategies = new CopyOnWriteArrayList<>();

    /**
     * Creates an empty registry. Documents then fall back to the MESSAGE strategy unless a custom one matches.
     */
    public Hl7V3StrategyRegistry() {
    }

    /**
     * Creates a new registry with the built-in strategies, in lookup order: C-CDA, then CDA R2, then HL7 v3 messages.
     * C-CDA comes first because a C-CDA document is also a CDA R2 document.
     *
     * @return a new registry; each call returns a separate instance
     */
    public static Hl7V3StrategyRegistry defaultRegistry() {
        Hl7V3StrategyRegistry registry = new Hl7V3StrategyRegistry();
        registry.strategies.add(new CcdaStrategy());
        registry.strategies.add(new CdaR2Strategy());
        registry.strategies.add(new Hl7V3MessageStrategy());
        return registry;
    }

    /**
     * Registers a custom strategy ahead of the existing ones. Ignored when the strategy is {@code null}.
     *
     * @param strategy the strategy to register
     */
    public void register(Hl7V3Strategy strategy) {
        if (strategy != null) {
            strategies.add(0, strategy);
        }
    }

    /**
     * Finds the first strategy, in lookup order, that supports the root element.
     *
     * @param root the root element of the document
     * @return the matching strategy, or an empty Optional when none supports it
     */
    public Optional<Hl7V3Strategy> find(Element root) {
        for (Hl7V3Strategy strategy : strategies) {
            if (strategy.supports(root)) {
                return Optional.of(strategy);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the registered strategies in lookup order.
     *
     * @return an unmodifiable snapshot of the strategies
     */
    public List<Hl7V3Strategy> strategies() {
        return Collections.unmodifiableList(new ArrayList<>(strategies));
    }
}
