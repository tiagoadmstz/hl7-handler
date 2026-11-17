package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategy;
import io.github.tdteixeira.hl7handler.version.Hl7VersionStrategyRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7VersionStrategySnapshotTest {

    private static final OffsetDateTime FIXED_TIMESTAMP = OffsetDateTime.of(2026, 1, 2, 3, 4, 5, 123456789, ZoneOffset.ofHours(-3));

    @Test
    @DisplayName("Snapshot da estratégia v2.1: MSH-9 simples, sem MSH-18, TS e EVN.1")
    void shouldSnapshotVersion21Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_1);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_1);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isFalse();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("EVN.1");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.2.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.2: MSH-9 com evento, sem MSH-18, TS e MSH.9.2")
    void shouldSnapshotVersion22Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_2);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_2);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isFalse();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.2.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.3: MSH-9 com evento, MSH-18 suportado, TS e PID.2.1")
    void shouldSnapshotVersion23Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_3);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_3);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.2.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.3.1: MSH-9 estrutural, TS e PID.3.1")
    void shouldSnapshotVersion231Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_3_1);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_3_1);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.4: MSH-9 estrutural, TS e PID.3.1")
    void shouldSnapshotVersion24Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_4);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_4);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.5: baseline, MSH-9 estrutural, TS e PID.3.1")
    void shouldSnapshotVersion25Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_5);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_5);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.5.1: MSH-9 estrutural, TS e PID.3.1")
    void shouldSnapshotVersion251Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_5_1);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_5_1);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.6: DTM com microssegundos e PID.3.1")
    void shouldSnapshotVersion26Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_6);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_6);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405.1234-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.7: truncamento suportado, até 5 caracteres de MSH-2 e DTM")
    void shouldSnapshotVersion27Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_7);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_7);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(5);
        assertThat(strategy.isTruncationSupported()).isTrue();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405.1234-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.7.1: truncamento suportado, até 5 caracteres de MSH-2 e DTM")
    void shouldSnapshotVersion271Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_7_1);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_7_1);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(5);
        assertThat(strategy.isTruncationSupported()).isTrue();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405.1234-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da estratégia v2.8: truncamento suportado, até 5 caracteres de MSH-2 e DTM")
    void shouldSnapshotVersion28Strategy() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_8);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_8);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(5);
        assertThat(strategy.isTruncationSupported()).isTrue();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405.1234-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot da versão 2.9 resolvida para a estratégia v2.8")
    void shouldSnapshotVersion29StrategyAsFallbackTo28() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.V2_9);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_8);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(5);
        assertThat(strategy.isTruncationSupported()).isTrue();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405.1234-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }

    @Test
    @DisplayName("Snapshot de versão desconhecida resolvida para a baseline v2.5")
    void shouldSnapshotUnknownVersionAsBaseline25() {
        Hl7VersionStrategy strategy = Hl7VersionStrategyRegistry.findStrategy(Hl7Version.UNKNOWN);

        assertThat(strategy.getVersion()).isEqualTo(Hl7Version.V2_5);
        assertThat(strategy.getMinEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.getMaxEncodingCharacters()).isEqualTo(4);
        assertThat(strategy.isTruncationSupported()).isFalse();
        assertThat(strategy.isMsh18Supported()).isTrue();
        assertThat(strategy.formatMsh9("A01", '^')).isEqualTo("ACK^A01^ACK");
        assertThat(strategy.formatHeaderTimestamp(FIXED_TIMESTAMP)).isEqualTo("20260102030405-0300");
        assertThat(strategy.getDefaultEventCoordinate()).isEqualTo("MSH.9.2");
        assertThat(strategy.getDefaultPatientIdCoordinate()).isEqualTo("PID.3.1");
    }
}
