package io.github.tiagoadmstz.hl7handler;

import io.github.tiagoadmstz.hl7handler.api.Hl7Processor;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Amostra de spot-check de monitor multiparamétrico (ORU_R01 2.6)")
class MonitorSpotCheckSampleTest {

    private static final String SAMPLE = "/samples/monitor/oru-spotcheck-multiparam-v26.hl7";
    private static final String MAPPING = "/mappings/monitor-spotcheck-mapping.yaml";

    private static Hl7Processor processor() {
        return Hl7Handler.builder().withMappingContent(TestResources.read(MAPPING)).build();
    }

    private static Map<String, Object> mapped() {
        return processor().read(TestResources.read(SAMPLE)).toMap();
    }

    private static Map<?, ?> sinal(int position) {
        List<?> sinais = (List<?>) mapped().get("sinais");
        return (Map<?, ?>) sinais.get(position - 1);
    }

    @Test
    @DisplayName("Deve ler a mensagem como 2.6, sem avisos")
    void shouldReadAsVersion26WithoutWarnings() {
        Hl7Message message = Hl7Handler.reader().from(TestResources.read(SAMPLE)).toMessage();

        assertThat(message.getVersion().getIdentifier()).isEqualTo("2.6");
        assertThat(message.warnings()).isEmpty();
        assertThat(message.getSegments("OBX")).hasSize(38);
    }

    @Test
    @DisplayName("Deve mapear o cabeçalho, o paciente e o leito do monitor")
    void shouldMapHeaderPatientAndBed() {
        Map<String, Object> result = mapped();

        assertThat(result.get("idMensagem")).isEqualTo("4");
        assertThat(result.get("dispositivo")).isEqualTo("0000000000000001");
        assertThat(result.get("prontuario")).isEqualTo("11");
        assertThat(result.get("equipamentoMac")).isEqualTo("00-00-00-00-00-01");
        assertThat(result.get("leito")).isEqualTo("LEITO-01");
        assertThat(result.get("tipoColeta")).isEqualTo("463890002");
    }

    @Test
    @DisplayName("Deve manter o fuso -0800 do gateway no horário da mensagem")
    void shouldKeepGatewayOffsetInMessageTime() {
        Object dataHora = mapped().get("dataHora");

        assertThat(dataHora).isInstanceOf(OffsetDateTime.class);
        OffsetDateTime value = (OffsetDateTime) dataHora;
        assertThat(value.getOffset()).isEqualTo(ZoneOffset.ofHours(-8));
        assertThat(value.toInstant()).isEqualTo(Instant.parse("2026-11-19T14:32:07Z"));
    }

    @Test
    @DisplayName("Deve mapear os 38 OBX com o tipo de cada valor")
    void shouldMapAllObxWithValueType() {
        List<?> sinais = (List<?>) mapped().get("sinais");

        assertThat(sinais).hasSize(38);
        assertThat(sinal(1).get("tipoValor")).isEqualTo("NM");
        assertThat(sinal(5).get("tipoValor")).isEqualTo("CNE");
        assertThat(sinal(33).get("tipoValor")).isEqualTo("ST");
    }

    @Test
    @DisplayName("Deve ler o valor numérico e a unidade de um OBX NM")
    void shouldReadNumericValueAndUnit() {
        Map<?, ?> temperatura = sinal(6);

        assertThat(temperatura.get("nome")).isEqualTo("MDC_TEMP_EAR");
        assertThat(temperatura.get("valor")).isEqualTo("37.2");
        assertThat(temperatura.get("unidade")).isEqualTo("MDC_DIM_DEGC");
    }

    @Test
    @DisplayName("Deve devolver o campo inteiro de um OBX CNE como texto")
    void shouldReturnWholeCneFieldAsText() {
        Map<?, ?> posicao = sinal(5);

        assertThat(posicao.get("nome")).isEqualTo("FICT_NIBP_ROUTINE_POS1");
        assertThat(posicao.get("valor")).isEqualTo("60001^FICT_POS_SITTING^99FICT");
    }

    @Test
    @DisplayName("Deve decodificar o escape de quebra de linha e os acentos do texto do NEWS2")
    void shouldDecodeLineBreakEscapeAndAccentsOfNews2Text() {
        String texto = String.valueOf(sinal(34).get("valor"));

        assertThat(texto).startsWith("**Title**NEWS2 Escore=8");
        assertThat(texto).contains("\n**Text***Enfermeiro");
        assertThat(texto).contains("equipe médica responsável");
        assertThat(texto).doesNotContain("\\X0A\\");
    }

    @Test
    @DisplayName("Deve responder ACK AA com o MSH-10 da mensagem do gateway")
    void shouldAcknowledgeWithMessageControlId() {
        Hl7Message message = Hl7Handler.reader().from(TestResources.read(SAMPLE)).toMessage();

        Hl7Message ack = Hl7Handler.reader().from(Hl7Handler.generateAck(message)).toMessage();

        assertThat(ack.get("MSA.1")).isEqualTo("AA");
        assertThat(ack.get("MSA.2")).isEqualTo("4");
    }

    @Test
    @DisplayName("Deve falhar ao converter em número o valor de um OBX CNE")
    void shouldFailToConvertCneValueToNumber() {
        String mapping = "mappings:\n"
            + "  sinais:\n"
            + "    lista: \"OBX[*]\"\n"
            + "    item:\n"
            + "      valor: { hl7: \"5\", tipo: numero }\n";
        Hl7Processor numeric = Hl7Handler.builder().withMappingContent(mapping).build();

        assertThatThrownBy(() -> numeric.read(TestResources.read(SAMPLE)).toMap())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("60001^FICT_POS_SITTING^99FICT");
    }

    @Test
    @DisplayName("Deve mapear os 38 OBX pelo tipo do OBX-2 sem erro, com número no NM e texto no CNE e no ST")
    void shouldMapAllObxByTypeCoordinateWithoutError() {
        String mapping = "mappings:\n"
            + "  sinais:\n"
            + "    lista: \"OBX[*]\"\n"
            + "    item:\n"
            + "      nome: \"3.2\"\n"
            + "      valor: { hl7: \"5\", tipoPor: \"2\" }\n";
        Hl7Processor byType = Hl7Handler.builder().withMappingContent(mapping).build();

        List<?> sinais = (List<?>) byType.read(TestResources.read(SAMPLE)).toMap().get("sinais");

        assertThat(sinais).hasSize(38);
        assertThat(((Map<?, ?>) sinais.get(0)).get("valor")).isEqualTo(119L);
        assertThat(((Map<?, ?>) sinais.get(4)).get("valor")).isEqualTo("60001^FICT_POS_SITTING^99FICT");
        assertThat(((Map<?, ?>) sinais.get(5)).get("valor")).isEqualTo(37.2);
        assertThat(((Map<?, ?>) sinais.get(32)).get("valor")).isEqualTo("NEWS2");
    }

    @Test
    @DisplayName("Deve converter a temperatura de 37.2 °C do OBX 6 para 99.0 °F e trocar a unidade por MDC_DIM_FAHR")
    void shouldConvertSampleTemperatureToFahrenheitAndAdjustUnit() {
        String mapping = "temperatura: fahrenheit\n"
            + "mappings:\n"
            + "  sinais:\n"
            + "    lista: \"OBX[*]\"\n"
            + "    item:\n"
            + "      nome: \"3.2\"\n"
            + "      valor: { hl7: \"5\", tipoPor: \"2\", unidadePor: \"6\" }\n"
            + "      unidade: { hl7: \"6.2\", ajustarUnidade: true }\n";
        Hl7Processor fahrenheit = Hl7Handler.builder().withMappingContent(mapping).build();

        List<?> sinais = (List<?>) fahrenheit.read(TestResources.read(SAMPLE)).toMap().get("sinais");
        Map<?, ?> temperatura = (Map<?, ?>) sinais.get(5);

        assertThat(temperatura.get("nome")).isEqualTo("MDC_TEMP_EAR");
        assertThat(temperatura.get("valor")).isEqualTo(99.0);
        assertThat(temperatura.get("unidade")).isEqualTo("MDC_DIM_FAHR");
        assertThat(((Map<?, ?>) sinais.get(0)).get("valor")).isEqualTo(119L);
        assertThat(((Map<?, ?>) sinais.get(0)).get("unidade")).isEqualTo("MDC_DIM_MMHG");
    }
}
