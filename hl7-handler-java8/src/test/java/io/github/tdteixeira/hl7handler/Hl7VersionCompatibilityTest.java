package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.model.Hl7Field;
import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Segment;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7VersionCompatibilityTest {

    private static final String SAMPLES = "/samples/versions/";

    private static String sample(String fileName) {
        return TestResources.read(SAMPLES + fileName);
    }

    private static Hl7Message parse(String fileName) {
        return Hl7Handler.reader().from(sample(fileName)).toMessage();
    }

    private static InputStream mapping() {
        return Hl7VersionCompatibilityTest.class.getResourceAsStream("/mappings/paciente-multiversao-mapping.yaml");
    }

    @Test
    @DisplayName("2.1: detecta a versão, MSH-9 só com o tipo e evento no EVN-1")
    void shouldReadVersion21WithEventInEvn() {
        Hl7Message message = parse("v21-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_1);
        assertThat(message.get("MSH.9")).isEqualTo("ADT");
        assertThat(message.get("EVN.1")).isEqualTo("A01");
        assertThat(message.get("PID.5.1")).isEqualTo("PEREIRA");
    }

    @Test
    @DisplayName("2.2: detecta a versão e lê tipo e evento do MSH-9")
    void shouldReadVersion22MessageTypeAndEvent() {
        Hl7Message message = parse("v22-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_2);
        assertThat(message.get("MSH.9.1")).isEqualTo("ADT");
        assertThat(message.get("MSH.9.2")).isEqualTo("A01");
    }

    @Test
    @DisplayName("2.3: detecta a versão e lê o MSH-18 e o ID externo no PID-2")
    void shouldReadVersion23CharsetAndExternalId() {
        Hl7Message message = parse("v23-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_3);
        assertThat(message.get("MSH.18")).isEqualTo("8859/1");
        assertThat(message.get("PID.2")).isEqualTo("EXT-230001");
        assertThat(message.get("PID.11.3")).isEqualTo("CURITIBA");
    }

    @Test
    @DisplayName("2.3.1: detecta a versão e lê a estrutura no MSH-9.3 e o segundo OBX")
    void shouldReadVersion231MessageStructureAndRepeatedObx() {
        Hl7Message message = parse("v231-oru.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_3_1);
        assertThat(message.get("MSH.9.3")).isEqualTo("ORU_R01");
        assertThat(message.getSegments("OBX")).hasSize(2);
        assertThat(message.get("OBX[2].5")).isEqualTo("185");
    }

    @Test
    @DisplayName("2.4: detecta a versão")
    void shouldDetectVersion24() {
        Hl7Message message = parse("v24-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_4);
        assertThat(message.get("PV1.3.2")).isEqualTo("202");
    }

    @Test
    @DisplayName("2.5.1: detecta a versão com patch e lê data com fuso")
    void shouldDetectVersion251() {
        Hl7Message message = parse("v251-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_5_1);
        assertThat(message.get("MSH.7")).isEqualTo("20261110120000-0300");
        assertThat(message.get("MSH.18")).isEqualTo("UNICODE UTF-8");
    }

    @Test
    @DisplayName("2.6 Mindray: detecta a versão e lê EUI-64, charset, perfil IHE e os OBX")
    void shouldReadVersion26MindrayObservation() {
        Hl7Message message = parse("v26-mindray-oru.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_6);
        assertThat(message.get("MSH.3.2")).isEqualTo("00A0370027E84537");
        assertThat(message.get("MSH.18")).isEqualTo("UNICODE UTF-8");
        assertThat(message.get("MSH.21.1")).isEqualTo("IHE_PCD_001");
        assertThat(message.get("OBX.3.2")).isEqualTo("MDC_PULS_OXIM_SAT_O2");
        assertThat(message.get("OBX.5")).isEqualTo("97");
        assertThat(message.get("OBX.18.1")).isEqualTo("00A0370027E84537");
        assertThat(message.get("OBX[2].5")).isEqualTo("72");
    }

    @Test
    @DisplayName("2.7: detecta a versão com MSH-2 de 5 caracteres (truncamento)")
    void shouldReadVersion27WithTruncationCharacter() {
        Hl7Message message = parse("v27-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_7);
        assertThat(message.get("MSH.2")).isEqualTo("^~\\&#");
        assertThat(message.get("MSH.10")).isEqualTo("V27-0001");
        assertThat(message.get("PID.5.1")).isEqualTo("ROCHA");
    }

    @Test
    @DisplayName("2.8: faz o parse e preserva o texto da versão no MSH-12")
    void shouldParseVersion28() {
        Hl7Message message = parse("v28-adt.hl7");

        assertThat(message.get("MSH.12")).isEqualTo("2.8");
        assertThat(message.get("PID.5.1")).isEqualTo("MARTINS");
    }

    @Test
    @DisplayName("2.9: faz o parse e preserva o texto da versão no MSH-12")
    void shouldParseVersion29() {
        Hl7Message message = parse("v29-adt.hl7");

        assertThat(message.get("MSH.12")).isEqualTo("2.9");
        assertThat(message.get("PID.5.1")).isEqualTo("NUNES");
    }

    @Test
    @DisplayName("Sem MSH-12: versão desconhecida e o parse continua")
    void shouldParseMessageWithoutVersion() {
        Hl7Message message = parse("no-msh12-adt.hl7");

        assertThat(message.getVersion()).isEqualTo(Hl7Version.UNKNOWN);
        assertThat(message.get("MSH.10")).isEqualTo("NOVER-0001");
        assertThat(message.get("PID.5.1")).isEqualTo("SILVA");
    }

    @Test
    @DisplayName("MSH-2 curto: separador de campo e de componente continuam corretos")
    void shouldParseShortEncodingCharacters() {
        Hl7Message message = parse("short-msh2-adt.hl7");

        assertThat(message.get("MSH.10")).isEqualTo("SHORT-0001");
        assertThat(message.get("PID.5.1")).isEqualTo("SOUZA");
        assertThat(message.get("PID.5.2")).isEqualTo("ANA");
    }

    @Test
    @DisplayName("Terminador CR e LF produzem a mesma estrutura")
    void shouldParseCarriageReturnAndLineFeedTheSameWay() {
        String withLineFeed = sample("v24-adt.hl7");
        String withCarriageReturn = withLineFeed.replace("\n", "\r");

        Hl7Message fromLineFeed = Hl7Handler.reader().from(withLineFeed).toMessage();
        Hl7Message fromCarriageReturn = Hl7Handler.reader().from(withCarriageReturn).toMessage();

        assertThat(fromCarriageReturn.getSegments()).hasSameSizeAs(fromLineFeed.getSegments());
        assertThat(fromCarriageReturn.getVersion()).isEqualTo(Hl7Version.V2_4);
        assertThat(fromCarriageReturn.get("PID.5.1")).isEqualTo(fromLineFeed.get("PID.5.1"));
    }

    @Test
    @DisplayName("Linhas em branco são ignoradas e campos além do padrão são preservados")
    void shouldIgnoreBlankLinesAndKeepExtraFields() {
        Hl7Message message = parse("blank-lines-and-extra-fields-adt.hl7");

        assertThat(message.getSegments()).hasSize(3);
        Hl7Segment pid = message.getSegment("PID").orElseThrow(IllegalStateException::new);
        List<Hl7Field> fields = pid.getFields();
        assertThat(fields.get(fields.size() - 1).getValue()).isEqualTo("CAMPO_EXTRA");
        assertThat(message.get("PID.5.1")).isEqualTo("OLIVEIRA");
    }

    @Test
    @DisplayName("O mesmo mapeamento YAML funciona na 2.3")
    void shouldApplySameMappingToVersion23() {
        String json = Hl7Handler.reader()
                .from(sample("v23-adt.hl7"))
                .withMapping(mapping())
                .toJson();

        assertThat(json)
                .contains("\"mensagemId\" : \"V23-0001\"")
                .contains("\"versaoHl7\" : \"2.3\"")
                .contains("\"sobrenome\" : \"FERREIRA\"");
    }

    @Test
    @DisplayName("O mesmo mapeamento YAML funciona na 2.6")
    void shouldApplySameMappingToVersion26() {
        String json = Hl7Handler.reader()
                .from(sample("v26-mindray-oru.hl7"))
                .withMapping(mapping())
                .toJson();

        assertThat(json)
                .contains("\"mensagemId\" : \"V26-0001\"")
                .contains("\"versaoHl7\" : \"2.6\"")
                .contains("\"prontuario\" : \"260001\"")
                .contains("\"sobrenome\" : \"TESTE\"");
    }
}
