package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.api.Hl7Reader;
import io.github.tdteixeira.hl7handler.model.Hl7DateTime;
import io.github.tdteixeira.hl7handler.model.Hl7Delimiters;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.model.Hl7VersionProfile;
import io.github.tdteixeira.hl7handler.parser.Hl7Parser;
import io.github.tdteixeira.hl7handler.parser.Hl7ParserRegistry;
import io.github.tdteixeira.hl7handler.parser.Hl7VersionDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7VersionProfileAndDateTimeTest {

    @Test
    @DisplayName("Hl7Version é um tipo aberto e comparável com constantes preservadas")
    void shouldProvideOpenComparableVersionValues() {
        Hl7Version v29 = Hl7Version.fromIdentifier("2.9");
        assertThat(v29.isUnknown()).isFalse();
        assertThat(v29.major()).isEqualTo(2);
        assertThat(v29.minor()).isEqualTo(9);
        assertThat(v29.patch()).isEqualTo(0);
        assertThat(v29.raw()).isEqualTo("2.9");
        assertThat(v29).isEqualTo(Hl7Version.V2_9);

        Hl7Version vCustom = Hl7Version.fromIdentifier("2.10.1");
        assertThat(vCustom.major()).isEqualTo(2);
        assertThat(vCustom.minor()).isEqualTo(10);
        assertThat(vCustom.patch()).isEqualTo(1);
        assertThat(vCustom.compareTo(Hl7Version.V2_9)).isGreaterThan(0);

        assertThat(Hl7Version.V2_1.compareTo(Hl7Version.V2_2)).isLessThan(0);
        assertThat(Hl7Version.V2_3.compareTo(Hl7Version.V2_3_1)).isLessThan(0);
        assertThat(Hl7Version.V2_8.compareTo(Hl7Version.V2_8_2)).isLessThan(0);
        assertThat(Hl7Version.UNKNOWN.compareTo(Hl7Version.V2_1)).isLessThan(0);

        List<Hl7Version> values = java.util.Arrays.asList(Hl7Version.values());
        assertThat(values).contains(Hl7Version.V2_8, Hl7Version.V2_8_1, Hl7Version.V2_8_2, Hl7Version.V2_9, Hl7Version.UNKNOWN);
    }

    @Test
    @DisplayName("Hl7VersionProfile carrega configurações dos arquivos properties")
    void shouldLoadVersionProfilesFromProperties() {
        Hl7VersionProfile p21 = Hl7VersionProfile.profileFor(Hl7Version.V2_1);
        assertThat(p21.getVersion()).isEqualTo(Hl7Version.V2_1);
        assertThat(p21.getDateType()).isEqualTo("TS");
        assertThat(p21.getMsh9Format()).isEqualTo("MSG");
        assertThat(p21.isMsh18Supported()).isFalse();
        assertThat(p21.getMsh2MinLength()).isEqualTo(4);
        assertThat(p21.getMsh2MaxLength()).isEqualTo(4);
        assertThat(p21.isMsh2TruncationSupported()).isFalse();

        Hl7VersionProfile p23 = Hl7VersionProfile.profileFor(Hl7Version.V2_3);
        assertThat(p23.isMsh18Supported()).isTrue();
        assertThat(p23.getMsh9Format()).isEqualTo("MSG_EVENT");

        Hl7VersionProfile p26 = Hl7VersionProfile.profileFor(Hl7Version.V2_6);
        assertThat(p26.getDateType()).isEqualTo("DTM");

        Hl7VersionProfile p27 = Hl7VersionProfile.profileFor(Hl7Version.V2_7);
        assertThat(p27.getMsh2MaxLength()).isEqualTo(5);
        assertThat(p27.isMsh2TruncationSupported()).isTrue();

        Hl7VersionProfile p28 = Hl7VersionProfile.profileFor(Hl7Version.V2_8);
        assertThat(p28.getDateType()).isEqualTo("DTM");
        assertThat(p28.isMsh2TruncationSupported()).isTrue();
    }

    @Test
    @DisplayName("Hl7VersionProfile aplica fallback para o perfil mais próximo abaixo")
    void shouldFallbackToNearestLowerProfileForNewerVersions() {
        Hl7VersionProfile p29 = Hl7VersionProfile.profileFor(Hl7Version.fromIdentifier("2.9"));
        assertThat(p29.getVersion()).isEqualTo(Hl7Version.V2_8);
        assertThat(p29.isFallbackFor(Hl7Version.fromIdentifier("2.9"))).isTrue();

        Hl7VersionProfile p282 = Hl7VersionProfile.profileFor(Hl7Version.V2_8_2);
        assertThat(p282.getVersion()).isEqualTo(Hl7Version.V2_8);
        assertThat(p282.isFallbackFor(Hl7Version.V2_8_2)).isTrue();

        Hl7VersionProfile p28 = Hl7VersionProfile.profileFor(Hl7Version.V2_8);
        assertThat(p28.isFallbackFor(Hl7Version.V2_8)).isFalse();
    }

    @Test
    @DisplayName("Registra avisos de conformidade quando houver fallback ou versão desconhecida")
    void shouldRecordWarningsWhenVersionOrProfileRequiresFallback() {
        Hl7Message msg29 = Hl7Handler.reader().from(TestResources.read("/samples/versions/v29-adt.hl7")).toMessage();
        assertThat(msg29.getVersion()).isEqualTo(Hl7Version.V2_9);
        assertThat(msg29.hasWarnings()).isTrue();
        assertThat(msg29.getWarnings()).anyMatch(w -> w.contains("2.9") && w.contains("2.8"));

        Hl7Message msgNoVer = Hl7Handler.reader().from(TestResources.read("/samples/versions/no-msh12-adt.hl7")).toMessage();
        assertThat(msgNoVer.getVersion()).isEqualTo(Hl7Version.UNKNOWN);
        assertThat(msgNoVer.hasWarnings()).isTrue();
        assertThat(msgNoVer.getWarnings()).anyMatch(w -> w.toLowerCase().contains("unknown"));

        Hl7Message msg28 = Hl7Handler.reader().from(TestResources.read("/samples/versions/v28-adt.hl7")).toMessage();
        assertThat(msg28.getVersion()).isEqualTo(Hl7Version.V2_8);
        assertThat(msg28.hasWarnings()).isFalse();
    }

    @Test
    @DisplayName("Hl7DateTime realiza parse com precisões variáveis e fusos horários")
    void shouldParseHl7DateTimeWithVariablePrecisionAndOffsets() {
        TemporalAccessor t4 = Hl7DateTime.parse("2026");
        assertThat(t4).isInstanceOf(LocalDate.class);
        assertThat((LocalDate) t4).isEqualTo(LocalDate.of(2026, 1, 1));

        TemporalAccessor t6 = Hl7DateTime.parse("202611");
        assertThat(t6).isInstanceOf(LocalDate.class);
        assertThat((LocalDate) t6).isEqualTo(LocalDate.of(2026, 11, 1));

        TemporalAccessor t8 = Hl7DateTime.parse("20261110");
        assertThat(t8).isInstanceOf(LocalDate.class);
        assertThat((LocalDate) t8).isEqualTo(LocalDate.of(2026, 11, 10));

        TemporalAccessor t12 = Hl7DateTime.parse("202611101430");
        assertThat(t12).isInstanceOf(LocalDateTime.class);
        assertThat((LocalDateTime) t12).isEqualTo(LocalDateTime.of(2026, 11, 10, 14, 30));

        TemporalAccessor t14 = Hl7DateTime.parse("20261110143025");
        assertThat(t14).isInstanceOf(LocalDateTime.class);
        assertThat((LocalDateTime) t14).isEqualTo(LocalDateTime.of(2026, 11, 10, 14, 30, 25));

        TemporalAccessor tFrac = Hl7DateTime.parse("20261110143025.500");
        assertThat(tFrac).isInstanceOf(LocalDateTime.class);
        assertThat(((LocalDateTime) tFrac).getNano()).isEqualTo(500000000);

        TemporalAccessor tMindray = Hl7DateTime.parse("20261014094500.0000-0300");
        assertThat(tMindray).isInstanceOf(OffsetDateTime.class);
        OffsetDateTime odt = (OffsetDateTime) tMindray;
        assertThat(odt.getOffset()).isEqualTo(ZoneOffset.ofHours(-3));
        assertThat(odt.toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 10, 14, 9, 45, 0));

        assertThat(Hl7DateTime.parseLocalDate("20261014094500.0000-0300")).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(Hl7DateTime.parseLocalDateTime("20261014094500.0000-0300")).isEqualTo(LocalDateTime.of(2026, 10, 14, 9, 45, 0));
        assertThat(Hl7DateTime.parseOffsetDateTime("20261110120000").getOffset()).isEqualTo(ZoneOffset.UTC);

        assertThatThrownBy(() -> Hl7DateTime.parse("")).isInstanceOf(DateTimeParseException.class);
        assertThatThrownBy(() -> Hl7DateTime.parse("20")).isInstanceOf(DateTimeParseException.class);
    }

    @Test
    @DisplayName("Detecta MSH e faz parse ignorando cabeçalhos FHS, BHS e lixo no preâmbulo")
    void shouldDetectMshAndParseThroughFhsBhsAndGarbagePreamble() {
        String batchHl7 = "FHS|^~\\&|FILE_SENDER|FILE_FACILITY|HL7_HANDLER|LOCAL|20261110||||\r"
                + "BHS|^~\\&|BATCH_SENDER|BATCH_FACILITY|HL7_HANDLER|LOCAL|20261110||||\r"
                + "MSH|^~\\&|APP_A|FAC_A|APP_B|FAC_B|20261110120000||ADT^A01^ADT_A01|BATCH-01|P|2.5\r"
                + "PID|1||12345||TESTE^BATCH\r";

        Hl7Version version = Hl7VersionDetector.detect(batchHl7);
        assertThat(version).isEqualTo(Hl7Version.V2_5);

        Hl7Message message = Hl7Handler.reader().from(batchHl7).toMessage();
        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_5);
        assertThat(message.get("PID.5.1")).isEqualTo("TESTE");

        String garbageHl7 = "--- START OF TRANSMISSION ---\r\n"
                + "MSH|^~\\&|APP_A|FAC_A|APP_B|FAC_B|20261110120000||ADT^A01^ADT_A01|PRE-01|P|2.6\r"
                + "PID|1||67890||TESTE^PREAMBLE\r";

        Hl7Version vGarbage = Hl7VersionDetector.detect(garbageHl7);
        assertThat(vGarbage).isEqualTo(Hl7Version.V2_6);

        Hl7Message msgGarbage = Hl7Handler.reader().from(garbageHl7).toMessage();
        assertThat(msgGarbage.getVersion()).isEqualTo(Hl7Version.V2_6);
        assertThat(msgGarbage.get("PID.5.1")).isEqualTo("TESTE");
    }

    @Test
    @DisplayName("DefaultHl7Reader armazena cache da mensagem para evitar reparses múltiplos")
    void shouldCacheParsedMessageInDefaultHl7Reader() {
        Hl7Reader reader = Hl7Handler.reader().from(TestResources.read("/samples/versions/v251-adt.hl7"));
        Hl7Message msg1 = reader.toMessage();
        Hl7Message msg2 = reader.toMessage();
        assertThat(msg1).isSameAs(msg2);

        String json = reader.toRawJson();
        assertThat(json).contains("2.5.1");

        reader.from("MSH|^~\\&|SYS||||20261110||ADT^A01|MSG99|P|2.4\rPID|1||99||NOVO^PACIENTE\r");
        Hl7Message msg3 = reader.toMessage();
        assertThat(msg3).isNotSameAs(msg1);
        assertThat(msg3.getVersion()).isEqualTo(Hl7Version.V2_4);
    }

    @Test
    @DisplayName("Hl7Handler.builder() permite isolamento sem alterar o registro singleton global")
    void shouldAllowConfiguringIsolatedBuilderWithoutModifyingGlobalRegistry() {
        Hl7ParserRegistry globalRegistry = Hl7ParserRegistry.getInstance();

        Hl7Parser mockParser = new Hl7Parser() {
            @Override
            public boolean supports(Hl7Version version) {
                return version.equals(Hl7Version.V2_5);
            }

            @Override
            public Hl7Message parse(String rawHl7) {
                Hl7Segment seg = new Hl7Segment("MSH");
                return new Hl7Message(Hl7Version.V2_5, Hl7Delimiters.DEFAULT, Collections.<Hl7Segment>singletonList(seg));
            }

            @Override
            public Hl7Message parse(String rawHl7, Hl7Version detectedVersion) {
                return parse(rawHl7);
            }
        };

        Hl7Reader isolatedReader = Hl7Handler.builder()
                .parser(mockParser)
                .charset(StandardCharsets.ISO_8859_1)
                .defaultDelimiters(Hl7Delimiters.DEFAULT)
                .build();

        assertThat(globalRegistry.getParserFor(Hl7Version.V2_5)).isNotSameAs(mockParser);

        Hl7Message parsed = isolatedReader.from("MSH|^~\\&|SYS||||20261110||ADT^A01|MSG01|P|2.5\r").toMessage();
        assertThat(parsed.getSegments()).hasSize(1);
    }
}
