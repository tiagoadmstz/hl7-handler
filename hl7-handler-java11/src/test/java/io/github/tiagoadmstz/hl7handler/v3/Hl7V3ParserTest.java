package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Namespaces;
import io.github.tiagoadmstz.hl7handler.v3.parser.Hl7V3Parser;
import io.github.tiagoadmstz.hl7handler.v3.version.Hl7V3StrategyRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Hl7V3ParserTest {

    private static final Hl7V3Parser PARSER =
        new Hl7V3Parser(Hl7V3StrategyRegistry.defaultRegistry(), new Hl7V3Namespaces(null));

    private static Hl7V3Document parseText(String xml) {
        return PARSER.parse(new InputSource(new StringReader(xml)));
    }

    @Test
    @DisplayName("Deve detectar um documento CDA R2 pela raiz ClinicalDocument")
    void shouldDetectCdaDocument() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.format()).isEqualTo(Hl7V3Format.CDA);
    }

    @Test
    @DisplayName("Deve detectar uma mensagem HL7 v3 pelo interactionId")
    void shouldDetectMessageDocument() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/prpa-in201301uv02.xml"));

        assertThat(document.format()).isEqualTo(Hl7V3Format.MESSAGE);
    }

    @Test
    @DisplayName("Deve devolver CDA-R2 como versão de um documento CDA")
    void shouldReadCdaVersionIdentifier() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.versionIdentifier()).isEqualTo("CDA-R2");
    }

    @Test
    @DisplayName("Deve devolver o versionCode como versão de uma mensagem")
    void shouldReadMessageVersionCodeAsVersionIdentifier() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/prpa-in201301uv02.xml"));

        assertThat(document.versionIdentifier()).isEqualTo("V3-2008N");
    }

    @Test
    @DisplayName("Deve devolver o valor do caminho existente, relativo à raiz")
    void shouldReturnValueOfExistingPath() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.get("hl7:title")).isEqualTo("Sumario de Alta Hospitalar");
    }

    @Test
    @DisplayName("Deve devolver string vazia quando o caminho não existe")
    void shouldReturnEmptyStringForMissingPath() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.get("hl7:naoExiste")).isEmpty();
    }

    @Test
    @DisplayName("Deve devolver todos os valores de um caminho repetido, na ordem do documento")
    void shouldReturnAllValuesOfRepeatedPath() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.getAll("hl7:recordTarget/hl7:patientRole/hl7:id/@extension"))
            .containsExactly("700000000000001", "PRT-12345");
    }

    @Test
    @DisplayName("Deve devolver lista vazia quando nenhum nó é selecionado")
    void shouldReturnEmptyListForMissingPath() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.getAll("hl7:naoExiste")).isEmpty();
    }

    @Test
    @DisplayName("Deve devolver a lista de templateId da raiz")
    void shouldReturnTemplateIds() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.templateIds()).containsExactly("2.16.840.1.113883.10.20.22.1.8");
    }

    @Test
    @DisplayName("Não deve emitir aviso para um CDA com typeId correto")
    void shouldHaveNoWarningsForValidCda() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/cda-r2-sumario-alta.xml"));

        assertThat(document.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve avisar quando o typeId do CDA diverge do padrão POCD_HD000040")
    void shouldWarnWhenCdaTypeIdDiverges() {
        String xml = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\">"
            + "<typeId root=\"2.16.840.1.113883.1.3\" extension=\"POCD_HD000999\"/>"
            + "<title>Teste</title>"
            + "</ClinicalDocument>";

        Hl7V3Document document = parseText(xml);

        assertThat(document.warnings())
            .containsExactly("CDA typeId is not 2.16.840.1.113883.1.3/POCD_HD000040");
    }

    @Test
    @DisplayName("Deve avisar que a mensagem não tem interactionId")
    void shouldWarnWhenMessageHasNoInteractionId() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/mcci-sem-interaction-id.xml"));

        assertThat(document.warnings()).contains("HL7 v3 message has no interactionId");
    }

    @Test
    @DisplayName("Deve avisar quando o ITSVersion da mensagem não é XML_1.0")
    void shouldWarnWhenMessageItsVersionDiffers() {
        Hl7V3Document document = parseText(V3TestResources.read("/samples/v3/mcci-sem-interaction-id.xml"));

        assertThat(document.warnings()).contains("ITSVersion 'XML_2.0' is not XML_1.0");
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException com a causa para XML malformado")
    void shouldRejectMalformedXml() {
        String xml = "<ClinicalDocument xmlns=\"urn:hl7-org:v3\"><title>Teste</ClinicalDocument>";

        assertThatThrownBy(() -> parseText(xml))
            .isInstanceOf(Hl7ParseException.class)
            .hasMessageStartingWith("Malformed HL7 v3 XML")
            .hasCauseInstanceOf(SAXException.class);
    }

    @Test
    @DisplayName("Deve recusar uma raiz fora do namespace urn:hl7-org:v3")
    void shouldRejectRootOutsideHl7V3Namespace() {
        String xml = V3TestResources.read("/samples/v3/xml-nao-hl7.xml");

        assertThatThrownBy(() -> parseText(xml))
            .isInstanceOf(Hl7ParseException.class)
            .hasMessageContaining("Not an HL7 v3 document");
    }

    @Test
    @DisplayName("Deve recusar DOCTYPE com entidade externa sem vazar o conteúdo do arquivo local")
    void shouldRejectDoctypeWithExternalEntityWithoutLeakingFileContent() {
        byte[] bytes = V3TestResources.bytes("/samples/v3/xxe-entidade-externa.xml");

        assertThatThrownBy(() -> PARSER.parse(new InputSource(new ByteArrayInputStream(bytes))))
            .isInstanceOf(Hl7ParseException.class)
            .hasMessageNotContaining("[fonts]")
            .hasMessageNotContaining("16-bit")
            .hasMessageNotContaining("win.ini");
    }

    @Test
    @DisplayName("Deve decodificar ISO-8859-1 pela declaração do XML ao ler bytes")
    void shouldParseIso88591FromByteStream() {
        byte[] bytes = V3TestResources.bytes("/samples/v3/cda-r2-iso-8859-1.xml");

        Hl7V3Document document = PARSER.parse(new InputSource(new ByteArrayInputStream(bytes)));

        assertThat(document.get("hl7:title")).isEqualTo("Avaliação clínica");
        assertThat(document.get("hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:given")).isEqualTo("José");
        assertThat(document.get("hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name/hl7:family")).isEqualTo("Conceição");
    }
}
