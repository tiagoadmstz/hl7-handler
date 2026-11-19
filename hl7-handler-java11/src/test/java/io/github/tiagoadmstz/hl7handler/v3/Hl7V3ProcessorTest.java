package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7V3ProcessorTest {

    private String cdaXml;
    private String cdaMapping;

    public static class PacienteVO {
        private String nome;
        private String sobrenome;
        private String sexo;

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getSobrenome() { return sobrenome; }
        public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }
        public String getSexo() { return sexo; }
        public void setSexo(String sexo) { this.sexo = sexo; }
    }

    public static class ExameVO {
        private String codigo;
        private Double valor;

        public String getCodigo() { return codigo; }
        public void setCodigo(String codigo) { this.codigo = codigo; }
        public Double getValor() { return valor; }
        public void setValor(Double valor) { this.valor = valor; }
    }

    public static class SumarioVO {
        private String documentoId;
        private String titulo;
        private PacienteVO paciente;
        private List<ExameVO> exames;

        public String getDocumentoId() { return documentoId; }
        public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public PacienteVO getPaciente() { return paciente; }
        public void setPaciente(PacienteVO paciente) { this.paciente = paciente; }
        public List<ExameVO> getExames() { return exames; }
        public void setExames(List<ExameVO> exames) { this.exames = exames; }
    }

    @BeforeEach
    void setUp() {
        cdaXml = V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml");
        cdaMapping = V3TestResources.read("/mappings/v3/cda-sumario-mapping.yaml");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @Test
    @DisplayName("Deve devolver JSON com os valores mapeados quando há mapeamento")
    void shouldReturnMappedJsonWhenMappingConfigured() {
        Hl7V3Processor processor = Hl7Handler.v3().withMappingContent(cdaMapping).build();

        String json = processor.read(cdaXml).toJson();

        assertThat(json)
            .contains("\"documentoId\"")
            .contains("Sumario de Alta Hospitalar")
            .doesNotContain("ClinicalDocument");
    }

    @Test
    @DisplayName("Deve devolver o JSON estrutural bruto quando não há mapeamento")
    void shouldReturnRawJsonWhenNoMappingConfigured() {
        Hl7V3Processor processor = Hl7Handler.v3().build();

        String json = processor.read(cdaXml).toJson();

        assertThat(json).contains("\"ClinicalDocument\"");
    }

    @Test
    @DisplayName("Deve lançar Hl7MappingException no toMap quando não há mapeamento")
    void shouldRejectToMapWithoutMapping() {
        Hl7V3Result result = Hl7Handler.v3().build().read(cdaXml);

        assertThatThrownBy(result::toMap).isInstanceOf(Hl7MappingException.class);
    }

    @Test
    @DisplayName("Deve informar que há mapeamento apenas quando um YAML foi configurado")
    void shouldReportHasMappingWhenConfigured() {
        assertThat(Hl7Handler.v3().withMappingContent(cdaMapping).build().hasMapping()).isTrue();
    }

    @Test
    @DisplayName("Deve informar que não há mapeamento quando nenhum YAML foi configurado")
    void shouldReportNoMappingWhenNotConfigured() {
        assertThat(Hl7Handler.v3().build().hasMapping()).isFalse();
    }

    @Test
    @DisplayName("Deve devolver o JSON estrutural com nome da raiz, atributos e texto")
    void shouldReturnRawJsonWithRootNameAttributesAndText() {
        String xml = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\">"
            + "<title lang=\"pt-BR\">Sumario de Alta Hospitalar</title>"
            + "<id root=\"2.16.840.1.113883.19.5\" extension=\"DOC-0001\"/>"
            + "</ClinicalDocument>";

        String json = Hl7Handler.v3().build().read(xml).toRawJson();

        assertThat(json)
            .contains("\"ClinicalDocument\"")
            .contains("\"@extension\" : \"DOC-0001\"")
            .contains("\"@lang\" : \"pt-BR\"")
            .contains("\"#text\" : \"Sumario de Alta Hospitalar\"");
    }

    @Test
    @DisplayName("Deve vincular os valores mapeados a um Value Object")
    void shouldBindMappedValuesToVo() {
        Hl7V3Processor processor = Hl7Handler.v3().withMappingContent(cdaMapping).build();

        SumarioVO vo = processor.read(cdaXml).bindTo(SumarioVO.class);

        assertThat(vo.getDocumentoId()).isEqualTo("DOC-0001");
        assertThat(vo.getTitulo()).isEqualTo("Sumario de Alta Hospitalar");
        assertThat(vo.getPaciente().getNome()).isEqualTo("Maria");
        assertThat(vo.getPaciente().getSobrenome()).isEqualTo("Ficticia");
        assertThat(vo.getPaciente().getSexo()).isEqualTo("F");
        assertThat(vo.getExames()).hasSize(2);
        assertThat(vo.getExames().get(0).getCodigo()).isEqualTo("2345-7");
        assertThat(vo.getExames().get(0).getValor()).isEqualTo(98.0);
    }

    @Test
    @DisplayName("Deve ler bytes ISO-8859-1 e decodificar os acentos pela declaração do XML")
    void shouldReadBytesWithIso88591Accents() {
        byte[] bytes = V3TestResources.bytes("/samples/v3/cda-r2-iso-8859-1.xml");
        Hl7V3Processor processor = Hl7Handler.v3().build();

        Hl7V3Document document = processor.readBytes(bytes).document();

        assertThat(document.get("hl7:title")).isEqualTo("Avaliação clínica");
    }

    @Test
    @DisplayName("Deve ler stream ISO-8859-1 e decodificar nome com acento")
    void shouldReadInputStreamWithIso88591Accents() {
        byte[] bytes = V3TestResources.bytes("/samples/v3/cda-r2-iso-8859-1.xml");
        Hl7V3Processor processor = Hl7Handler.v3().build();

        Hl7V3Document document = processor.read(new ByteArrayInputStream(bytes)).document();

        assertThat(document.get("hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:given")).isEqualTo("José");
        assertThat(document.get("hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:family")).isEqualTo("Conceição");
    }

    @Test
    @DisplayName("Deve mapear uma mensagem HL7 v3 com o mapeamento de paciente")
    void shouldMapMessageWithPatientMapping() {
        String xml = V3TestResources.read("/samples/v3/prpa-in201301uv02.xml");
        String mapping = V3TestResources.read("/mappings/v3/prpa-paciente-mapping.yaml");

        Map<String, Object> map = Hl7Handler.v3().withMappingContent(mapping).build().read(xml).toMap();

        assertThat(map.get("mensagemId")).isEqualTo("MSG-0001");
        assertThat(map.get("interacao")).isEqualTo("PRPA_IN201301UV02");
        assertThat(map.get("remetente")).isEqualTo("2.16.840.1.113883.19.3.1");
        assertThat(asMap(map.get("paciente")).get("id")).isEqualTo("PAC-777");
        assertThat(asMap(map.get("paciente")).get("nome")).isEqualTo("Ana");
    }

    @Test
    @DisplayName("Deve recusar uma string nula com Hl7ParseException")
    void shouldRejectNullString() {
        Hl7V3Processor processor = Hl7Handler.v3().build();

        assertThatThrownBy(() -> processor.read((String) null)).isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve recusar uma string em branco com Hl7ParseException")
    void shouldRejectBlankString() {
        Hl7V3Processor processor = Hl7Handler.v3().build();

        assertThatThrownBy(() -> processor.read("   \n\t ")).isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve recusar um array de bytes vazio com Hl7ParseException")
    void shouldRejectEmptyBytes() {
        Hl7V3Processor processor = Hl7Handler.v3().build();

        assertThatThrownBy(() -> processor.readBytes(new byte[0])).isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve devolver o mesmo mapa em leituras concorrentes no mesmo processor")
    void shouldProduceEqualMapsForConcurrentReads() throws Exception {
        Hl7V3Processor processor = Hl7Handler.v3().withMappingContent(cdaMapping).build();
        final String xml = cdaXml;
        Map<String, Object> expected = processor.read(xml).toMap();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Map<String, Object>>> futures = new ArrayList<>();
            for (int i = 0; i < 400; i++) {
                futures.add(executor.submit(() -> processor.read(xml).toMap()));
            }
            for (Future<Map<String, Object>> future : futures) {
                assertThat(future.get()).isEqualTo(expected);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Deve devolver um toMap não modificável")
    void shouldReturnUnmodifiableMap() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build().read(cdaXml).toMap();

        assertThatThrownBy(() -> map.put("novo", "valor")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Deve devolver listas aninhadas do toMap também não modificáveis")
    @SuppressWarnings("unchecked")
    void shouldReturnUnmodifiableNestedList() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build().read(cdaXml).toMap();
        List<Object> exames = (List<Object>) map.get("exames");

        assertThatThrownBy(() -> exames.add("novo")).isInstanceOf(UnsupportedOperationException.class);
    }
}
