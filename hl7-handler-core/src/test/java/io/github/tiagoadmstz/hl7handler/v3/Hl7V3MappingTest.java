package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.mapping.Hl7MappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7V3MappingTest {

    private static final String CDA_XML_PATH = "/samples/v3/cda-r2-sumario-alta.xml";

    private String cdaXml;
    private String cdaMapping;

    @BeforeEach
    void setUp() {
        cdaXml = V3TestResources.read(CDA_XML_PATH);
        cdaMapping = V3TestResources.read("/mappings/v3/cda-sumario-mapping.yaml");
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

    @Test
    @DisplayName("Deve mapear um caminho XPath de texto para o valor do nó")
    void shouldMapXPathStringValue() {
        String mapping = yaml("mappings:", "  titulo: \"hl7:title\"");

        Map<String, Object> map = Hl7Handler.v3().withMappingContent(mapping).build()
            .read(cdaXml).toMap();

        assertThat(map.get("titulo")).isEqualTo("Sumario de Alta Hospitalar");
    }

    @Test
    @DisplayName("Deve usar o fallback quando o caminho principal está vazio")
    void shouldUseFallbackWhenPrimaryPathIsEmpty() {
        String xml = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\">"
            + "<custodian><assignedCustodian><representedCustodianOrganization>"
            + "<telecom value=\"tel:+55-00-0000-0000\"/>"
            + "</representedCustodianOrganization></assignedCustodian></custodian>"
            + "</ClinicalDocument>";
        String mapping = yaml("mappings:",
            "  telefone: { xpath: \"hl7:recordTarget/hl7:patientRole/hl7:telecom/@value\", fallback: \"hl7:custodian//hl7:telecom/@value\" }");

        Map<String, Object> map = Hl7Handler.v3().withMappingContent(mapping).build().read(xml).toMap();

        assertThat(map.get("telefone")).isEqualTo("tel:+55-00-0000-0000");
    }

    @Test
    @DisplayName("Deve devolver null quando o caminho e o fallback estão ambos ausentes")
    void shouldReturnNullWhenPathAndFallbackAreBothEmpty() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        assertThat(asMap(map.get("paciente")).get("telefone")).isNull();
    }

    @Test
    @DisplayName("Deve usar o padrão quando o caminho está vazio")
    void shouldUsePadraoWhenPathIsEmpty() {
        String mapping = yaml("mappings:",
            "  sexo: { xpath: \"hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:naoExiste/@code\", padrao: \"U\" }");

        Map<String, Object> map = Hl7Handler.v3().withMappingContent(mapping).build().read(cdaXml).toMap();

        assertThat(map.get("sexo")).isEqualTo("U");
    }

    @Test
    @DisplayName("Deve devolver o valor constante declarado na diretiva valor")
    void shouldReturnConstantValue() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        assertThat(map.get("origem")).isEqualTo("CDA");
    }

    @Test
    @DisplayName("Deve converter a data do tipo data em OffsetDateTime com o fuso do documento")
    void shouldConvertDataTypeToOffsetDateTime() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        assertThat(map.get("emissao"))
            .isEqualTo(OffsetDateTime.of(2026, 11, 9, 14, 30, 0, 0, ZoneOffset.ofHours(-3)));
    }

    @Test
    @DisplayName("Deve converter a data de nascimento do tipo data em LocalDate")
    void shouldConvertBirthDateToLocalDate() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        assertThat(asMap(map.get("paciente")).get("nascimento")).isEqualTo(LocalDate.of(1980, 1, 15));
    }

    @Test
    @DisplayName("Deve converter um número inteiro do tipo numero em Long")
    void shouldConvertIntegerNumberToLong() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        Object valor = asMap(asList(map.get("exames")).get(0)).get("valor");
        assertThat(valor).isInstanceOf(Long.class).isEqualTo(98L);
    }

    @Test
    @DisplayName("Deve converter um número decimal do tipo numero em Double")
    void shouldConvertDecimalNumberToDouble() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        Object valor = asMap(asList(map.get("exames")).get(1)).get("valor");
        assertThat(valor).isInstanceOf(Double.class).isEqualTo(13.5);
    }

    @Test
    @DisplayName("Deve converter o valor do tipo booleano")
    void shouldConvertBooleanType() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        assertThat(asMap(map.get("paciente")).get("falecido")).isEqualTo(Boolean.FALSE);
    }

    @Test
    @DisplayName("Deve transformar cada nó da lista em um item com caminhos relativos")
    void shouldMapListWithRelativeItemPaths() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        List<Object> exames = asList(map.get("exames"));
        assertThat(exames).hasSize(2);
        assertThat(asMap(exames.get(0)).get("codigo")).isEqualTo("2345-7");
        assertThat(asMap(exames.get(0)).get("descricao")).isEqualTo("Glicose");
        assertThat(asMap(exames.get(0)).get("unidade")).isEqualTo("mg/dL");
        assertThat(asMap(exames.get(1)).get("codigo")).isEqualTo("718-7");
    }

    @Test
    @DisplayName("Deve resolver a coordenada semântica #name dentro do mapeamento")
    void shouldResolveSemanticCoordinateInsideMapping() {
        Map<String, Object> map = Hl7Handler.v3().withMappingContent(cdaMapping).build()
            .read(cdaXml).toMap();

        assertThat(map.get("documentoId")).isEqualTo("DOC-0001");
        assertThat(asMap(map.get("paciente")).get("id")).isEqualTo("700000000000001");
    }

    @Test
    @DisplayName("Deve devolver null para valor vazio por padrão")
    void shouldReturnNullForEmptyValueByDefault() {
        String mapping = yaml("mappings:", "  ausente: \"hl7:naoExiste\"");

        Map<String, Object> map = Hl7Handler.v3().withMappingContent(mapping).build().read(cdaXml).toMap();

        assertThat(map).containsKey("ausente");
        assertThat(map.get("ausente")).isNull();
    }

    @Test
    @DisplayName("Deve devolver string vazia para valor vazio quando keepEmptyStrings está ativo")
    void shouldKeepEmptyStringWhenConfigured() {
        String mapping = yaml("mappings:", "  ausente: \"hl7:naoExiste\"");

        Map<String, Object> map = Hl7Handler.v3().keepEmptyStrings(true).withMappingContent(mapping).build()
            .read(cdaXml).toMap();

        assertThat(map.get("ausente")).isEqualTo("");
    }

    @Test
    @DisplayName("Deve aplicar o conversor customizado registrado no builder")
    void shouldApplyCustomConverter() {
        String mapping = yaml("mappings:", "  titulo: { xpath: \"hl7:title\", tipo: CPF }");

        Map<String, Object> map = Hl7Handler.v3()
            .converter("cpf", raw -> "CPF:" + raw)
            .withMappingContent(mapping)
            .build()
            .read(cdaXml)
            .toMap();

        assertThat(map.get("titulo")).isEqualTo("CPF:Sumario de Alta Hospitalar");
    }

    @Test
    @DisplayName("Deve recusar no build um XPath inválido, informando o caminho do nó no YAML")
    void shouldRejectInvalidXPathWithNodePath() {
        String mapping = yaml("mappings:", "  paciente:", "    nome: \"hl7:name[\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("mappings.paciente.nome");
    }

    @Test
    @DisplayName("Deve recusar no build um tipo desconhecido, informando o caminho do nó no YAML")
    void shouldRejectUnknownTipoWithNodePath() {
        String mapping = yaml("mappings:", "  origem: { xpath: \"hl7:title\", tipo: inexistente }");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("inexistente")
            .hasMessageContaining("mappings.origem");
    }

    @Test
    @DisplayName("Deve recusar no build um formato que não é CDA nem MESSAGE")
    void shouldRejectInvalidFormato() {
        String mapping = yaml("formato: XML", "mappings:", "  titulo: \"hl7:title\"");

        assertThatThrownBy(() -> Hl7Handler.v3().withMappingContent(mapping).build())
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("Invalid format 'XML' at 'formato'");
    }

    @Test
    @DisplayName("Deve lançar Hl7MappingException no toMap quando o formato do mapeamento diverge do documento")
    void shouldRejectMessageMappingAgainstCdaDocument() {
        String mapping = yaml("formato: MESSAGE", "mappings:", "  titulo: \"hl7:title\"");
        Hl7V3Result result = Hl7Handler.v3().withMappingContent(mapping).build().read(cdaXml);

        assertThatThrownBy(result::toMap)
            .isInstanceOf(Hl7MappingException.class)
            .hasMessageContaining("does not match mapping format");
    }

    @Test
    @DisplayName("Deve usar o prefixo de namespace declarado no bloco namespaces do YAML")
    void shouldUseNamespaceDeclaredInYaml() {
        String xml = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\" xmlns:ext=\"urn:exemplo\">"
            + "<ext:codigo>XYZ-1</ext:codigo>"
            + "</ClinicalDocument>";
        String mapping = yaml("namespaces:", "  ext: \"urn:exemplo\"", "mappings:", "  codigo: \"ext:codigo\"");

        Map<String, Object> map = Hl7Handler.v3().withMappingContent(mapping).build().read(xml).toMap();

        assertThat(map.get("codigo")).isEqualTo("XYZ-1");
    }

    @Test
    @DisplayName("Deve usar o prefixo de namespace declarado pelo builder")
    void shouldUseNamespaceDeclaredByBuilder() {
        String xml = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\" xmlns:ext=\"urn:exemplo\">"
            + "<ext:codigo>XYZ-2</ext:codigo>"
            + "</ClinicalDocument>";
        String mapping = yaml("mappings:", "  codigo: \"ext:codigo\"");

        Map<String, Object> map = Hl7Handler.v3().namespace("ext", "urn:exemplo")
            .withMappingContent(mapping).build().read(xml).toMap();

        assertThat(map.get("codigo")).isEqualTo("XYZ-2");
    }
}
