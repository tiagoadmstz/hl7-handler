package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.api.Hl7Processor;
import io.github.tdteixeira.hl7handler.api.Hl7Result;
import io.github.tdteixeira.hl7handler.mapping.Hl7MappingException;
import io.github.tdteixeira.hl7handler.network.MllpFramingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7ProcessorTest {

    private String sampleHl7;
    private String mappingYaml;

    @BeforeEach
    void setUp() {
        sampleHl7 = TestResources.read("/samples/sample-v25.hl7");
        mappingYaml = TestResources.read("/mappings/paciente-mapping.yaml");
    }

    private Hl7Processor processorWithMapping() {
        return Hl7Handler.builder().withMappingContent(mappingYaml).build();
    }

    @Test
    @DisplayName("Deve ter Hl7Exception como superclasse de Hl7ParseException")
    void shouldExtendHl7ExceptionWithParseException() {
        assertThat(Hl7ParseException.class.getSuperclass()).isEqualTo(Hl7Exception.class);
    }

    @Test
    @DisplayName("Deve ter Hl7Exception como superclasse de Hl7BindingException")
    void shouldExtendHl7ExceptionWithBindingException() {
        assertThat(Hl7BindingException.class.getSuperclass()).isEqualTo(Hl7Exception.class);
    }

    @Test
    @DisplayName("Deve ter Hl7Exception como superclasse de Hl7MappingException")
    void shouldExtendHl7ExceptionWithMappingException() {
        assertThat(Hl7MappingException.class.getSuperclass()).isEqualTo(Hl7Exception.class);
    }

    @Test
    @DisplayName("Deve ter Hl7Exception como superclasse de MllpFramingException")
    void shouldExtendHl7ExceptionWithMllpFramingException() {
        assertThat(MllpFramingException.class.getSuperclass()).isEqualTo(Hl7Exception.class);
    }

    @Test
    @DisplayName("Deve ler a mensagem pelo processador")
    void shouldReadMessageThroughProcessor() {
        Hl7Result result = processorWithMapping().read(sampleHl7);

        assertThat(result.message().get("PID.5.1")).isEqualTo("SILVA");
        assertThat(result.message().get("MSH.10")).isEqualTo("MSG202611090001");
    }

    @Test
    @DisplayName("Deve gerar o mapa semântico pelo processador")
    void shouldMapToMapThroughProcessor() {
        Map<String, Object> map = processorWithMapping().read(sampleHl7).toMap();

        assertThat(map).containsEntry("mensagemId", "MSG202611090001");
        assertThat(map.get("paciente")).asInstanceOf(MAP).containsEntry("nome", "JOAO");
    }

    @Test
    @DisplayName("Deve gerar JSON mapeado pelo processador")
    void shouldRenderMappedJsonThroughProcessor() {
        String json = processorWithMapping().read(sampleHl7).toJson();

        assertThat(json)
                .contains("\"mensagemId\" : \"MSG202611090001\"")
                .contains("\"nome\" : \"JOAO\"");
    }

    @Test
    @DisplayName("Deve fazer bind no VO pelo processador")
    void shouldBindToVoThroughProcessor() {
        PacienteAdmissaoVO vo = processorWithMapping().read(sampleHl7).bindTo(PacienteAdmissaoVO.class);

        assertThat(vo.getPaciente().getNome()).isEqualTo("JOAO");
        assertThat(vo.getInternacao().getLeito()).isEqualTo("101");
    }

    @Test
    @DisplayName("Deve gerar JSON bruto quando o processador não tem mapeamento")
    void shouldRenderRawJsonWhenProcessorHasNoMapping() {
        Hl7Processor processor = Hl7Handler.builder().build();

        String json = processor.read(sampleHl7).toJson();

        assertThat(json).contains("\"segments\"");
    }

    @Test
    @DisplayName("Deve informar que há mapeamento quando configurado")
    void shouldReportMappingWhenConfigured() {
        assertThat(processorWithMapping().hasMapping()).isTrue();
    }

    @Test
    @DisplayName("Deve informar que não há mapeamento quando não configurado")
    void shouldReportNoMappingWhenNotConfigured() {
        assertThat(Hl7Handler.builder().build().hasMapping()).isFalse();
    }

    @Test
    @DisplayName("Deve devolver a mesma instância do mapa em chamadas repetidas de toMap")
    void shouldReturnSameMapInstanceOnRepeatedToMap() {
        Hl7Result result = processorWithMapping().read(sampleHl7);

        assertThat(result.toMap()).isSameAs(result.toMap());
    }

    @Test
    @DisplayName("Deve devolver mapa não modificável em toMap")
    void shouldReturnUnmodifiableMap() {
        Hl7Result result = processorWithMapping().read(sampleHl7);

        assertThatThrownBy(() -> result.toMap().put("novo", "valor"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7MappingException em toMap quando não há mapeamento")
    void shouldThrowMappingExceptionOnToMapWithoutMapping() {
        Hl7Result result = Hl7Handler.builder().build().read(sampleHl7);

        assertThatThrownBy(result::toMap)
                .isInstanceOf(Hl7MappingException.class)
                .hasMessageContaining("No YAML mapping was configured");
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException quando a entrada está em branco")
    void shouldThrowParseExceptionOnBlankInput() {
        Hl7Processor processor = processorWithMapping();

        assertThatThrownBy(() -> processor.read("   "))
                .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException quando a entrada é nula")
    void shouldThrowParseExceptionOnNullInput() {
        Hl7Processor processor = processorWithMapping();

        assertThatThrownBy(() -> processor.read(null))
                .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve falhar no build quando o YAML de mapeamento é inválido")
    void shouldFailBuildOnInvalidYaml() {
        String invalidYaml = "nome: [\n";

        assertThatThrownBy(() -> Hl7Handler.builder().withMappingContent(invalidYaml).build())
                .isInstanceOf(Hl7MappingException.class);
    }

    @Test
    @DisplayName("Deve ler bytes com enquadramento MLLP pelo processador")
    void shouldReadBytesThroughProcessor() {
        byte[] framed = Hl7Handler.network().wrap(sampleHl7);

        Hl7Result result = processorWithMapping().readBytes(framed);

        assertThat(result.message().get("PID.5.1")).isEqualTo("SILVA");
    }

    @Test
    @DisplayName("Deve ler uma mensagem MLLP do stream pelo processador")
    void shouldReadMllpStreamThroughProcessor() {
        byte[] framed = Hl7Handler.network().wrap(sampleHl7);

        Hl7Result result = processorWithMapping().readMllp(new ByteArrayInputStream(framed));

        assertThat(result.toMap().get("paciente")).asInstanceOf(MAP).containsEntry("nome", "JOAO");
    }

    @Test
    @DisplayName("Deve devolver null quando o stream MLLP está vazio")
    void shouldReturnNullWhenMllpStreamIsEmpty() {
        Hl7Result result = processorWithMapping().readMllp(new ByteArrayInputStream(new byte[0]));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve lançar MllpFramingException quando falta o byte de início MLLP")
    void shouldThrowFramingExceptionWhenMllpStartBlockIsMissing() {
        byte[] unframed = sampleHl7.getBytes(StandardCharsets.UTF_8);
        Hl7Processor processor = processorWithMapping();

        assertThatThrownBy(() -> processor.readMllp(new ByteArrayInputStream(unframed)))
                .isInstanceOf(MllpFramingException.class);
    }

    @Test
    @DisplayName("Deve devolver mapas idênticos quando um processador é usado por 8 threads em paralelo")
    void shouldReturnIdenticalMapsWhenSharedAcrossThreads() throws Exception {
        Hl7Processor processor = processorWithMapping();
        Map<String, Object> expected = processor.read(sampleHl7).toMap();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Map<String, Object>>> tasks = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                tasks.add(() -> processor.read(sampleHl7).toMap());
            }
            List<Future<Map<String, Object>>> futures = executor.invokeAll(tasks);
            for (Future<Map<String, Object>> future : futures) {
                assertThat(future.get()).isEqualTo(expected);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
