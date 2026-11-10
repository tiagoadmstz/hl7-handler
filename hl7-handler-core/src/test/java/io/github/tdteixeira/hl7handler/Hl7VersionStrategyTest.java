package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.mapping.Hl7ToMapTransformer;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.network.Hl7AckGenerator;
import io.github.tdteixeira.hl7handler.version.AbstractHl7VersionStrategy;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategy;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategyRegistry;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7VersionStrategyTest {

    private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.of(2026, 11, 10, 15, 30, 45, 123400000, ZoneOffset.ofHours(-3));

    @Test
    @DisplayName("Estratégia v2.1 formata MSH-9 simples, sem MSH-18 e usa coordenadas EVN.1 e PID.2.1")
    void shouldValidateHl7V21StrategyBehavior() {
        Hl7V21Strategy strategy = new Hl7V21Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_1);
        assertThat(strategy.supports(Hl7Version.V2_1)).isTrue();
        assertThat(strategy.supports(Hl7Version.V2_2)).isFalse();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK");
        assertThat(strategy.formatMsh9(null, '^')).isEqualTo("ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20261110153045-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("EVN.1");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.2.1");
        assertThat(strategy.isMsh18Supported()).isFalse();
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getProfile()).isNotNull();
    }

    @Test
    @DisplayName("Estratégia v2.2 formata MSH-9 com tipo e evento e busca evento em MSH.9.2")
    void shouldValidateHl7V22StrategyBehavior() {
        Hl7V22Strategy strategy = new Hl7V22Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_2);
        assertThat(strategy.supports(Hl7Version.V2_2)).isTrue();
        assertThat(strategy.supports(Hl7Version.V2_1)).isFalse();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01");
        assertThat(strategy.formatMsh9("", '^')).isEqualTo("ACK^R01");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.2.1");
        assertThat(strategy.isMsh18Supported()).isFalse();
    }

    @Test
    @DisplayName("Estratégia v2.3 ativa suporte ao MSH-18 e preserva PID.2.1")
    void shouldValidateHl7V23StrategyBehavior() {
        Hl7V23Strategy strategy = new Hl7V23Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_3);
        assertThat(strategy.supports(Hl7Version.V2_3)).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.2.1");
        assertThat(strategy.isMsh18Supported()).isTrue();
    }

    @Test
    @DisplayName("Estratégia v2.3.1 formata MSH-9 com 3 partes e direciona prontuário para PID.3.1")
    void shouldValidateHl7V231StrategyBehavior() {
        Hl7V231Strategy strategy = new Hl7V231Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_3_1);
        assertThat(strategy.supports(Hl7Version.V2_3_1)).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
        assertThat(strategy.isMsh18Supported()).isTrue();
    }

    @Test
    @DisplayName("Estratégia v2.4 herda contratos de v2.3.1 e representa versão 2.4")
    void shouldValidateHl7V24StrategyBehavior() {
        Hl7V24Strategy strategy = new Hl7V24Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_4);
        assertThat(strategy.supports(Hl7Version.V2_4)).isTrue();
        assertThat(strategy.formatMsh9("A04", '^')).isEqualTo("ACK^A04^ACK");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Estratégias v2.5 e v2.5.1 operam como baseline estável com formato TS")
    void shouldValidateHl7V25AndV251StrategiesBehavior() {
        Hl7V25Strategy strategy25 = new Hl7V25Strategy();
        Hl7V251Strategy strategy251 = new Hl7V251Strategy();

        assertThat(strategy25.getVersion()).isEqualTo(Hl7Version.V2_5);
        assertThat(strategy251.getVersion()).isEqualTo(Hl7Version.V2_5_1);
        assertThat(strategy25.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20261110153045-0300");
        assertThat(strategy251.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20261110153045-0300");
        assertThat(strategy25.isTruncationSupported()).isFalse();
        assertThat(strategy251.isTruncationSupported()).isFalse();
    }

    @Test
    @DisplayName("Estratégia v2.6 formata timestamp no padrão DTM com frações de segundo")
    void shouldValidateHl7V26StrategyBehavior() {
        Hl7V26Strategy strategy = new Hl7V26Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_6);
        assertThat(strategy.supports(Hl7Version.V2_6)).isTrue();
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20261110153045.1234-0300");
        assertThat(strategy.formatHeaderTimestamp(null)).isEmpty();
    }

    @Test
    @DisplayName("Estratégias v2.7 e v2.7.1 suportam caractere de truncamento e 5 caracteres de encoding")
    void shouldValidateHl7V27AndV271StrategiesBehavior() {
        Hl7V27Strategy strategy27 = new Hl7V27Strategy();
        Hl7V271Strategy strategy271 = new Hl7V271Strategy();

        assertThat(strategy27.getVersion()).isEqualTo(Hl7Version.V2_7);
        assertThat(strategy27.getMaxEncodingCharacters()).isEqualTo(5);
        assertThat(strategy27.isTruncationSupported()).isTrue();

        assertThat(strategy271.getVersion()).isEqualTo(Hl7Version.V2_7_1);
        assertThat(strategy271.getMaxEncodingCharacters()).isEqualTo(5);
        assertThat(strategy271.isTruncationSupported()).isTrue();
    }

    @Test
    @DisplayName("Estratégia v2.8 atende à versão 2.8 e serve como fallback para versões 2.8.x e 2.9")
    void shouldValidateHl7V28StrategyBehaviorAndFallback() {
        Hl7V28Strategy strategy = new Hl7V28Strategy();

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_8);
        assertThat(strategy.supports(Hl7Version.V2_8)).isTrue();
        assertThat(strategy.supports(Hl7Version.V2_8_1)).isTrue();
        assertThat(strategy.supports(Hl7Version.V2_8_2)).isTrue();
        assertThat(strategy.supports(Hl7Version.V2_9)).isTrue();
        assertThat(strategy.supports(Hl7Version.fromIdentifier("2.10"))).isTrue();
        assertThat(strategy.supports(Hl7Version.V2_7)).isFalse();
        assertThat(strategy.supports(null)).isFalse();
        assertThat(strategy.supports(Hl7Version.UNKNOWN)).isFalse();
    }

    @Test
    @DisplayName("Registro localiza estratégias exatas para todas as versões cadastradas")
    void shouldResolveExactStrategiesFromRegistry() {
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_1)).isInstanceOf(Hl7V21Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_2)).isInstanceOf(Hl7V22Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_3)).isInstanceOf(Hl7V23Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_3_1)).isInstanceOf(Hl7V231Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_4)).isInstanceOf(Hl7V24Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_5)).isInstanceOf(Hl7V25Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_5_1)).isInstanceOf(Hl7V251Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_6)).isInstanceOf(Hl7V26Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_7)).isInstanceOf(Hl7V27Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_7_1)).isInstanceOf(Hl7V271Strategy.class);
        assertThat(Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_8)).isInstanceOf(Hl7V28Strategy.class);
    }

    @Test
    @DisplayName("Registro aplica floor para versões intermediárias e baseline v2.5 para versão desconhecida")
    void shouldResolveFallbackStrategiesFromRegistry() {
        Hl7VersionStrategy s29 = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_9);
        assertThat(s29).isInstanceOf(Hl7V28Strategy.class);

        Hl7VersionStrategy s282 = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_8_2);
        assertThat(s282).isInstanceOf(Hl7V28Strategy.class);

        Hl7VersionStrategy sUnknown = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.UNKNOWN);
        assertThat(sUnknown).isInstanceOf(Hl7V25Strategy.class);

        Hl7VersionStrategy sNull = Hl7VersionStrategyRegistry.findStrategy(null);
        assertThat(sNull).isInstanceOf(Hl7V25Strategy.class);

        assertThat(Hl7VersionStrategyRegistry.getBaselineStrategy()).isInstanceOf(Hl7V25Strategy.class);
    }

    @Test
    @DisplayName("Permite registrar estratégia customizada no registro")
    void shouldAllowCustomStrategyRegistration() {
        Hl7Version customVersion = Hl7Version.fromIdentifier("2.99");
        Hl7VersionStrategy customStrategy = new AbstractHl7VersionStrategy(customVersion) {
            @Override
            public String formatMsh9(String eventTrigger, char componentSeparator) {
                return "CUSTOM_ACK";
            }
        };

        Hl7VersionStrategyRegistry.registerStrategy(customStrategy);
        Hl7VersionStrategy resolved = Hl7VersionStrategyRegistry.findStrategy(customVersion);

        assertThat(resolved).isSameAs(customStrategy);
        assertThat(resolved.formatMsh9("A01", '^')).isEqualTo("CUSTOM_ACK");
    }

    @Test
    @DisplayName("Lista de estratégias registradas contém todas as versões padrão")
    void shouldContainAllDefaultStrategiesInRegisteredList() {
        List<Hl7VersionStrategy> list = Hl7VersionStrategyRegistry.getRegisteredStrategies();
        assertThat(list).isNotEmpty();
        assertThat(list.size()).isGreaterThanOrEqualTo(11);
    }

    @Test
    @DisplayName("Hl7AckGenerator utiliza a estratégia para formatar cabeçalho e MSH-9 por versão")
    void shouldGenerateAckDelegatingToStrategy() {
        String raw21 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT|CTRL21|P|2.1\rEVN|A01|20261110\r";
        Hl7Message msg21 = Hl7Handler.reader().from(raw21).toMessage();
        String ack21 = Hl7AckGenerator.generateAck(msg21);
        assertThat(ack21).contains("|ACK|ACK-CTRL21|P|2.1\r");

        String raw26 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT^A01^ADT_A01|CTRL26|P|2.6\r";
        Hl7Message msg26 = Hl7Handler.reader().from(raw26).toMessage();
        String ack26 = Hl7AckGenerator.generateAck(msg26);
        assertThat(ack26).contains("|ACK^A01^ACK|ACK-CTRL26|P|2.6\r");
    }

    @Test
    @DisplayName("Hl7ToMapTransformer resolve coordenadas semânticas @event e @patientId via estratégia")
    void shouldResolveSemanticCoordinatesInMapTransformer() {
        String raw21 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT|CTRL21|P|2.1\r"
                + "EVN|A01|20261110\r"
                + "PID|1|EXT21|INT21||SILVA^JOSE\r";
        Hl7Message msg21 = Hl7Handler.reader().from(raw21).toMessage();

        String raw25 = "MSH|^~\\&|ORIG|FAC|DEST|FAC|20261110||ADT^A01^ADT_A01|CTRL25|P|2.5\r"
                + "PID|1|EXT25|INT25||SILVA^JOSE\r";
        Hl7Message msg25 = Hl7Handler.reader().from(raw25).toMessage();

        Map<String, Object> mapping = new HashMap<>();
        mapping.put("evento", "@event");
        mapping.put("pacienteId", "@patientId");

        Hl7ToMapTransformer transformer = new Hl7ToMapTransformer();

        Map<String, Object> result21 = transformer.transform(msg21, mapping);
        assertThat(result21.get("evento")).isEqualTo("A01");
        assertThat(result21.get("pacienteId")).isEqualTo("EXT21");

        Map<String, Object> result25 = transformer.transform(msg25, mapping);
        assertThat(result25.get("evento")).isEqualTo("A01");
        assertThat(result25.get("pacienteId")).isEqualTo("INT25");
    }
}
