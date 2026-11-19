package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import io.github.tiagoadmstz.hl7handler.v3.writer.Hl7V3Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Escritor HL7 v3 a partir de mapeamento YAML")
class Hl7V3WriterTest {

    private static final String CDA_MAPPING_PATH = "/mappings/v3/cda-escrita-mapping.yaml";
    private static final String PRPA_MAPPING_PATH = "/mappings/v3/prpa-escrita-mapping.yaml";
    private static final String TEMPLATE_PATH = "/samples/v3/template-cda-vazio.xml";
    private static final String CDA_XML_PATH = "/samples/v3/cda-r2-sumario-alta.xml";

    public static class DocumentoVO {
        private String documentoId;
        private String titulo;
        private String emissao;

        public String getDocumentoId() { return documentoId; }
        public void setDocumentoId(String documentoId) { this.documentoId = documentoId; }
        public String getTitulo() { return titulo; }
        public void setTitulo(String titulo) { this.titulo = titulo; }
        public String getEmissao() { return emissao; }
        public void setEmissao(String emissao) { this.emissao = emissao; }
    }

    private static String yaml(String... lines) {
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append('\n');
        }
        return text.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        return (List<Object>) value;
    }

    private static Map<String, Object> documentoCompleto() {
        Map<String, Object> paciente = new LinkedHashMap<>();
        paciente.put("id", "PAC-0100");
        paciente.put("cns", "700000000000010");
        paciente.put("prontuario", "PRT-0100");
        paciente.put("nome", "Maria");
        paciente.put("sobrenome", "Exemplo");
        paciente.put("sexo", "F");
        paciente.put("nascimento", LocalDate.of(1980, 1, 15));
        paciente.put("falecido", Boolean.FALSE);
        paciente.put("telefone", "tel:+55-00-0000-0000");

        Map<String, Object> autor = new LinkedHashMap<>();
        autor.put("registro", "CRM-0100");
        autor.put("nome", "Joao");

        Map<String, Object> glicose = new LinkedHashMap<>();
        glicose.put("codigo", "2345-7");
        glicose.put("descricao", "Glicose");
        glicose.put("valor", 98L);
        glicose.put("unidade", "mg/dL");

        Map<String, Object> hemoglobina = new LinkedHashMap<>();
        hemoglobina.put("codigo", "718-7");
        hemoglobina.put("descricao", "Hemoglobina");
        hemoglobina.put("valor", 13.5d);
        hemoglobina.put("unidade", "g/dL");

        Map<String, Object> documento = new LinkedHashMap<>();
        documento.put("documentoId", "DOC-0100");
        documento.put("titulo", "Sumário de Alta Hospitalar");
        documento.put("emissao", OffsetDateTime.of(2026, 11, 9, 14, 30, 0, 0, ZoneOffset.ofHours(-3)));
        documento.put("origem", "CDA");
        documento.put("paciente", paciente);
        documento.put("autor", autor);
        documento.put("exames", new ArrayList<>(List.of(glicose, hemoglobina)));
        return documento;
    }

    private static Hl7V3Writer cdaWriter() {
        return Hl7Handler.v3().withMappingContent(V3TestResources.read(CDA_MAPPING_PATH)).buildWriter();
    }

    private static Map<String, Object> readWith(String mapping, String xml) {
        return Hl7Handler.v3().withMappingContent(mapping).build().read(xml).toMap();
    }

    private static String valueOf(String xml, String xpath) {
        return Hl7Handler.v3().build().read(xml).document().get(xpath);
    }

    @Test
    @DisplayName("Deve escrever um CDA sem template usando raiz e relê-lo com o mesmo mapeamento")
    void shouldWriteCdaWithRaizAndReadItBack() {
        Map<String, Object> documento = documentoCompleto();

        String xml = cdaWriter().write(documento);

        assertThat(xml).contains("<ClinicalDocument");
        assertThat(readWith(V3TestResources.read(CDA_MAPPING_PATH), xml)).isEqualTo(documento);
    }

    @Test
    @DisplayName("Deve escrever a mensagem PRPA a partir de um Map e relê-la com o mesmo mapeamento")
    void shouldWriteMessageFromMapAndReadItBack() {
        Map<String, Object> paciente = new LinkedHashMap<>();
        paciente.put("id", "PAC-0200");
        paciente.put("nome", "Ana");
        paciente.put("sobrenome", "Teste");
        Map<String, Object> mensagem = new LinkedHashMap<>();
        mensagem.put("mensagemId", "MSG-0200");
        mensagem.put("paciente", paciente);

        String xml = Hl7Handler.v3().withMappingContent(V3TestResources.read(PRPA_MAPPING_PATH))
            .buildWriter().write(mensagem);

        assertThat(readWith(V3TestResources.read(PRPA_MAPPING_PATH), xml)).isEqualTo(mensagem);
        assertThat(Hl7Handler.v3().build().read(xml).document().format()).isEqualTo(Hl7V3Format.MESSAGE);
    }

    @Test
    @DisplayName("Deve preencher o template respeitando a ordem dos elementos do documento")
    void shouldFillTemplateInDocumentOrder() {
        String mapping = yaml(
            "mappings:",
            "  documentoId: \"hl7:id/@extension\"",
            "  titulo: \"hl7:title\"",
            "  emissao: { xpath: \"hl7:effectiveTime/@value\", tipo: data }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentoId", "DOC-0300");
        data.put("titulo", "Sumario");
        data.put("emissao", OffsetDateTime.of(2026, 11, 9, 14, 30, 0, 0, ZoneOffset.ofHours(-3)));

        String xml = Hl7Handler.v3().templateContent(V3TestResources.read(TEMPLATE_PATH))
            .withMappingContent(mapping).buildWriter().write(data);

        int typeId = xml.indexOf("<typeId");
        int templateId = xml.indexOf("<templateId");
        int id = xml.indexOf("<id ");
        int code = xml.indexOf("<code ");
        int title = xml.indexOf("<title>Sumario</title>");
        int effectiveTime = xml.indexOf("<effectiveTime");
        assertThat(typeId).isPositive();
        assertThat(templateId).isGreaterThan(typeId);
        assertThat(id).isGreaterThan(templateId);
        assertThat(code).isGreaterThan(id);
        assertThat(title).isGreaterThan(code);
        assertThat(effectiveTime).isGreaterThan(title);
        assertThat(readWith(mapping, xml)).isEqualTo(data);
    }

    @Test
    @DisplayName("Deve carregar o template a partir de um InputStream")
    void shouldLoadTemplateFromInputStream() {
        String mapping = yaml("mappings:", "  titulo: \"hl7:title\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", "Via stream");

        String xml = Hl7Handler.v3()
            .template(new ByteArrayInputStream(V3TestResources.bytes(TEMPLATE_PATH)))
            .withMappingContent(mapping)
            .buildWriter()
            .write(data);

        assertThat(valueOf(xml, "hl7:title")).isEqualTo("Via stream");
        assertThat(valueOf(xml, "hl7:typeId/@root")).isEqualTo("2.16.840.1.113883.1.3");
    }

    @Test
    @DisplayName("Deve carregar o template a partir de um Path")
    void shouldLoadTemplateFromPath() {
        String mapping = yaml("mappings:", "  titulo: \"hl7:title\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", "Via path");

        String xml = Hl7Handler.v3()
            .template(V3TestResources.path(TEMPLATE_PATH))
            .withMappingContent(mapping)
            .buildWriter()
            .write(data);

        assertThat(valueOf(xml, "hl7:title")).isEqualTo("Via path");
    }

    @Test
    @DisplayName("Deve criar o atributo do predicado [@attr='valor'] ao escrever o caminho")
    void shouldCreateAttributeFromPredicate() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  cns: \"hl7:recordTarget/hl7:patientRole/hl7:id[@root='2.16.840.1.113883.13.236']/@extension\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cns", "700000000000010");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml,
            "hl7:recordTarget/hl7:patientRole/hl7:id[@root='2.16.840.1.113883.13.236']/@extension"))
            .isEqualTo("700000000000010");
    }

    @Test
    @DisplayName("Deve escrever no n-ésimo elemento do passo posicional [n]")
    void shouldWriteNthElementOfPositionalStep() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  prontuario: \"hl7:recordTarget/hl7:patientRole/hl7:id[2]/@extension\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("prontuario", "PRT-0400");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        Hl7V3Document document = Hl7Handler.v3().build().read(xml).document();
        assertThat(document.get("hl7:recordTarget/hl7:patientRole/hl7:id[2]/@extension")).isEqualTo("PRT-0400");
        assertThat(document.getAll("hl7:recordTarget/hl7:patientRole/hl7:id")).hasSize(2);
    }

    @Test
    @DisplayName("Deve gerar um elemento por item de uma lista")
    void shouldCreateOneElementPerListItem() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  exames:",
            "    lista: \"hl7:component/hl7:section/hl7:entry/hl7:observation\"",
            "    item:",
            "      codigo: \"hl7:code/@code\"");
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("codigo", "A-1");
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("codigo", "B-2");
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("codigo", "C-3");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exames", new ArrayList<>(List.of(a, b, c)));

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        List<Object> exames = asList(readWith(mapping, xml).get("exames"));
        assertThat(exames).hasSize(3);
        assertThat(asMap(exames.get(2)).get("codigo")).isEqualTo("C-3");
    }

    @Test
    @DisplayName("Deve gravar o valor constante declarado com xpath")
    void shouldWriteConstantWithXPath() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  tipoRaiz: { valor: \"2.16.840.1.113883.1.3\", xpath: \"hl7:typeId/@root\" }");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(new LinkedHashMap<String, Object>());

        assertThat(valueOf(xml, "hl7:typeId/@root")).isEqualTo("2.16.840.1.113883.1.3");
    }

    @Test
    @DisplayName("Deve ignorar a constante sem xpath, que não tem onde ser gravada")
    void shouldSkipConstantWithoutXPath() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  origem: { valor: \"CDA\" }");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(new LinkedHashMap<String, Object>());

        assertThat(xml).doesNotContain("CDA");
    }

    @Test
    @DisplayName("Deve usar o padrão quando o valor é nulo")
    void shouldUsePadraoWhenValueIsNull() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  sexo: { xpath: \"hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:administrativeGenderCode/@code\", padrao: \"U\" }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sexo", null);

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml,
            "hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:administrativeGenderCode/@code")).isEqualTo("U");
    }

    @Test
    @DisplayName("Deve omitir o elemento quando o valor é nulo e não há padrão")
    void shouldOmitElementWhenValueIsNullWithoutPadrao() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  titulo: \"hl7:title\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", null);

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(xml).doesNotContain("<title");
    }

    @Test
    @DisplayName("Deve ignorar o fallback na escrita quando o valor é nulo")
    void shouldIgnoreFallbackWhenValueIsNull() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  telefone: { xpath: \"hl7:recordTarget/hl7:patientRole/hl7:telecom/@value\", fallback: \"hl7:custodian//hl7:telecom/@value\" }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("telefone", null);

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(xml).doesNotContain("telecom");
    }

    @Test
    @DisplayName("Deve converter LocalDate em TS no formato yyyyMMdd")
    void shouldWriteLocalDateAsDate() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:",
            "  nascimento: { xpath: \"hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:birthTime/@value\", tipo: data }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nascimento", LocalDate.of(1980, 1, 15));

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml, "hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:birthTime/@value"))
            .isEqualTo("19800115");
    }

    @Test
    @DisplayName("Deve converter OffsetDateTime em TS com fuso no formato yyyyMMddHHmmssZ")
    void shouldWriteOffsetDateTimeWithZone() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:",
            "  emissao: { xpath: \"hl7:effectiveTime/@value\", tipo: data }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("emissao", OffsetDateTime.of(1980, 1, 15, 10, 30, 0, 0, ZoneOffset.ofHours(-3)));

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml, "hl7:effectiveTime/@value")).isEqualTo("19800115103000-0300");
    }

    @Test
    @DisplayName("Deve converter uma string ISO com fuso em TS quando o tipo é data")
    void shouldConvertIsoStringWithOffsetToTimestamp() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:",
            "  emissao: { xpath: \"hl7:effectiveTime/@value\", tipo: data }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("emissao", "2026-11-09T14:30:00-03:00");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml, "hl7:effectiveTime/@value")).isEqualTo("20261109143000-0300");
    }

    @Test
    @DisplayName("Deve converter uma string ISO só com data em TS yyyyMMdd quando o tipo é data")
    void shouldConvertIsoDateStringToTimestamp() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:",
            "  nascimento: { xpath: \"hl7:birthTime/@value\", tipo: data }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nascimento", "2026-11-09");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml, "hl7:birthTime/@value")).isEqualTo("20261109");
    }

    @Test
    @DisplayName("Deve escrever BigDecimal sem notação científica")
    void shouldWriteBigDecimalAsPlainString() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  valor: \"hl7:value/@value\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("valor", new BigDecimal("1E+3"));

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(data);

        assertThat(valueOf(xml, "hl7:value/@value")).isEqualTo("1000");
    }

    @Test
    @DisplayName("Deve escrever um VO convertido em Map pelo Jackson")
    void shouldWriteValueObject() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  documentoId: \"hl7:id/@extension\"",
            "  titulo: \"hl7:title\"",
            "  emissao: { xpath: \"hl7:effectiveTime/@value\", tipo: data }");
        DocumentoVO vo = new DocumentoVO();
        vo.setDocumentoId("DOC-0200");
        vo.setTitulo("Sumario por VO");
        vo.setEmissao("2026-11-09T14:30:00-03:00");

        String xml = Hl7Handler.v3().withMappingContent(mapping).buildWriter().write(vo);

        Map<String, Object> read = readWith(mapping, xml);
        assertThat(read.get("documentoId")).isEqualTo("DOC-0200");
        assertThat(read.get("titulo")).isEqualTo("Sumario por VO");
        assertThat(read.get("emissao")).isEqualTo(OffsetDateTime.of(2026, 11, 9, 14, 30, 0, 0, ZoneOffset.ofHours(-3)));
    }

    @Test
    @DisplayName("Deve escrever os bytes em UTF-8 preservando os acentos")
    void shouldWriteUtf8BytesWithAccents() {
        Hl7V3Writer writer = cdaWriter();
        Map<String, Object> documento = documentoCompleto();

        byte[] bytes = writer.writeBytes(documento);

        assertThat(new String(bytes, StandardCharsets.UTF_8)).contains("Sumário de Alta Hospitalar");
        assertThat(bytes).isEqualTo(writer.write(documento).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Deve escrever sem indentação quando indent é falso")
    void shouldWriteWithoutIndentationWhenDisabled() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  titulo: \"hl7:title\"");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("titulo", "Sem indentacao");

        String xml = Hl7Handler.v3().withMappingContent(mapping).indent(false).buildWriter().write(data);

        assertThat(xml).doesNotContain("\n  ");
        assertThat(valueOf(xml, "hl7:title")).isEqualTo("Sem indentacao");
    }

    @Test
    @DisplayName("Deve reescrever o documento lido pelo toXml com o mesmo conteúdo")
    void shouldRoundTripThroughDocumentToXml() {
        String mapping = V3TestResources.read(CDA_MAPPING_PATH);
        Hl7V3Document lido = Hl7Handler.v3().build().read(V3TestResources.read(CDA_XML_PATH)).document();

        String regravado = lido.toXml();

        assertThat(regravado).startsWith("<?xml");
        assertThat(readWith(mapping, regravado)).isEqualTo(readWith(mapping, V3TestResources.read(CDA_XML_PATH)));
    }

    @Test
    @DisplayName("Deve recusar no buildWriter um caminho com descendente '//'")
    void shouldRejectDescendantPathOnBuild() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  titulo: \"hl7:recordTarget//hl7:name\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("'titulo'")
            .hasMessageContaining("descendant steps");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter uma raiz com quebra de linha no nome")
    void shouldRejectRootNameWithLineBreakOnBuild() {
        String mapping = yaml("raiz: \"Clinical\nDocument\"", "mappings:", "  titulo: \"hl7:title\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("Invalid 'raiz' value");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter uma função XPath, como concat")
    void shouldRejectFunctionCallOnBuild() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  titulo: \"concat(hl7:title, 'x')\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("function calls");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter a coordenada #documentId, que é um concat não gravável")
    void shouldRejectNonWritableCoordinateOnBuild() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  documentoId: \"#documentId\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("documentoId");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter um prefixo de namespace não declarado")
    void shouldRejectUnknownNamespacePrefixOnBuild() {
        String mapping = yaml("raiz: ClinicalDocument", "mappings:", "  titulo: \"abc:title\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("abc:title");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter uma lista que termina em atributo")
    void shouldRejectListEndingInAttributeOnBuild() {
        String mapping = yaml(
            "raiz: ClinicalDocument",
            "mappings:",
            "  exames:",
            "    lista: \"hl7:component/hl7:section/@id\"",
            "    item:",
            "      codigo: \"hl7:code/@code\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("must end with an element step");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter quando não há raiz nem template")
    void shouldRejectWriterWithoutRootOrTemplate() {
        String mapping = yaml("mappings:", "  titulo: \"hl7:title\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("'raiz'");
    }

    @Test
    @DisplayName("Deve recusar no buildWriter quando não há mapeamento")
    void shouldRejectWriterWithoutMapping() {
        assertThatThrownBy(() -> Hl7Handler.v3().buildWriter())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("withMapping");
    }

    @Test
    @DisplayName("Deve recusar dados nulos na escrita")
    void shouldRejectNullData() {
        Hl7V3Writer writer = cdaWriter();

        assertThatThrownBy(() -> writer.write((Map<String, Object>) null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Deve produzir XML idêntico em escritas concorrentes")
    void shouldProduceIdenticalXmlUnderConcurrentWrites() throws Exception {
        Hl7V3Writer writer = cdaWriter();
        Map<String, Object> documento = documentoCompleto();
        String esperado = writer.write(documento);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                futures.add(pool.submit(() -> writer.write(documento)));
            }
            for (Future<String> future : futures) {
                assertThat(future.get()).isEqualTo(esperado);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
