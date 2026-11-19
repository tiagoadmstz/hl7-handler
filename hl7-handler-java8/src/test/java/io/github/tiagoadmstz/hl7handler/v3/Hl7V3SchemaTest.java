package io.github.tiagoadmstz.hl7handler.v3;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.v3.writer.Hl7V3Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DisplayName("Validação de documentos HL7 v3 por XSD")
class Hl7V3SchemaTest {

    private static final String SCHEMA_PATH = "/schemas/v3/teste-documento.xsd";
    private static final String VALID_PATH = "/samples/v3/cda-xsd-valido.xml";
    private static final String INVALID_PATH = "/samples/v3/cda-xsd-invalido.xml";
    private static final String TEMPLATE_PATH = "/samples/v3/template-cda-vazio.xml";
    private static final String STRICT_FIRST_ERROR_PREFIX = "XSD line 6, column ";

    private static Hl7V3Document readWithSchema(String xml, Hl7V3SchemaMode mode) {
        return Hl7Handler.v3()
            .schema(V3TestResources.url(SCHEMA_PATH))
            .schemaValidation(mode)
            .build()
            .read(xml)
            .document();
    }

    private static String yaml(String... lines) {
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line).append('\n');
        }
        return text.toString();
    }

    @Test
    @DisplayName("Deve aceitar um documento válido sem erros e sem avisos")
    void shouldAcceptValidDocumentWithoutErrors() {
        Hl7V3Document document = readWithSchema(V3TestResources.read(VALID_PATH), Hl7V3SchemaMode.STRICT);

        assertThat(document.format()).isEqualTo(Hl7V3Format.CDA);
        assertThat(document.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve lançar Hl7V3SchemaValidationException no modo STRICT com a linha do erro")
    void shouldThrowInStrictModeWithLineOfError() {
        String xml = V3TestResources.read(INVALID_PATH);

        Hl7V3SchemaValidationException error = catchThrowableOfType(
            () -> readWithSchema(xml, Hl7V3SchemaMode.STRICT), Hl7V3SchemaValidationException.class);

        assertThat(error).isNotNull();
        assertThat(error.getMessage()).contains("does not match the XSD schema");
        assertThat(error.errors()).isNotEmpty();
        assertThat(error.errors().get(0)).startsWith(STRICT_FIRST_ERROR_PREFIX);
        assertThat(error.errors().get(0)).contains("effectiveTime");
    }

    @Test
    @DisplayName("Deve usar STRICT como modo padrão quando o modo não é informado")
    void shouldUseStrictModeByDefault() {
        String xml = V3TestResources.read(INVALID_PATH);

        assertThatThrownBy(() -> Hl7Handler.v3()
            .schema(V3TestResources.url(SCHEMA_PATH))
            .build()
            .read(xml))
            .isInstanceOf(Hl7V3SchemaValidationException.class);
    }

    @Test
    @DisplayName("Deve registrar o erro como aviso com a linha no modo WARN")
    void shouldAddWarningWithLineInWarnMode() {
        Hl7V3Document document = readWithSchema(V3TestResources.read(INVALID_PATH), Hl7V3SchemaMode.WARN);

        assertThat(document.warnings()).anySatisfy(warning -> assertThat(warning).startsWith(STRICT_FIRST_ERROR_PREFIX));
    }

    @Test
    @DisplayName("Deve resolver o xs:include relativo, aplicando as regras do tipo incluído")
    void shouldResolveRelativeIncludeAndApplyIncludedType() {
        String semRoot = V3TestResources.read(VALID_PATH).replace(
            "<typeId root=\"2.16.840.1.113883.1.3\" extension=\"POCD_HD000040\"/>",
            "<typeId extension=\"POCD_HD000040\"/>");

        assertThatThrownBy(() -> readWithSchema(semRoot, Hl7V3SchemaMode.STRICT))
            .isInstanceOf(Hl7V3SchemaValidationException.class);
        Hl7V3Document warned = readWithSchema(semRoot, Hl7V3SchemaMode.WARN);
        assertThat(warned.warnings()).anySatisfy(warning -> assertThat(warning).contains("'root'"));
    }

    @Test
    @DisplayName("Deve aceitar o XSD informado como Path")
    void shouldAcceptSchemaGivenAsPath() {
        Hl7V3Document document = Hl7Handler.v3()
            .schema(V3TestResources.path(SCHEMA_PATH))
            .build()
            .read(V3TestResources.read(VALID_PATH))
            .document();

        assertThat(document.warnings()).isEmpty();
    }

    @Test
    @DisplayName("Deve falhar no build com IllegalArgumentException quando o XSD não existe")
    void shouldFailBuildWhenSchemaFileIsMissing() {
        assertThatThrownBy(() -> Hl7Handler.v3()
            .schema(Paths.get("target", "xsd-inexistente", "nao-existe.xsd"))
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot load XSD schema");
    }

    @Test
    @DisplayName("Deve aceitar a saída válida do writer quando o XSD está configurado")
    void shouldAcceptValidWriterOutput() {
        String mapping = yaml(
            "mappings:",
            "  documentoId: \"hl7:id/@extension\"",
            "  titulo: \"hl7:title\"",
            "  emissao: { xpath: \"hl7:effectiveTime/@value\", tipo: data }");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentoId", "DOC-0500");
        data.put("titulo", "Sumario validado");
        data.put("emissao", OffsetDateTime.of(2026, 11, 9, 14, 30, 0, 0, ZoneOffset.ofHours(-3)));

        String xml = Hl7Handler.v3()
            .templateContent(V3TestResources.read(TEMPLATE_PATH))
            .withMappingContent(mapping)
            .schema(V3TestResources.url(SCHEMA_PATH))
            .buildWriter()
            .write(data);

        assertThat(xml).contains("Sumario validado");
    }

    @Test
    @DisplayName("Deve recusar no modo STRICT a saída do writer que não confere com o XSD")
    void shouldRejectInvalidWriterOutputInStrictMode() {
        Hl7V3Writer writer = writerWithRecordTarget(Hl7V3SchemaMode.STRICT);

        Hl7V3SchemaValidationException error = catchThrowableOfType(
            () -> writer.write(dataWithRecordTarget()), Hl7V3SchemaValidationException.class);

        assertThat(error).isNotNull();
        assertThat(error.errors().get(0)).contains("recordTarget");
    }

    @Test
    @DisplayName("Deve devolver a saída do writer com aviso no modo WARN mesmo fora do XSD")
    void shouldReturnInvalidWriterOutputInWarnMode() {
        Hl7V3Writer writer = writerWithRecordTarget(Hl7V3SchemaMode.WARN);

        String xml = writer.write(dataWithRecordTarget());

        assertThat(xml).contains("recordTarget");
    }

    private static Hl7V3Writer writerWithRecordTarget(Hl7V3SchemaMode mode) {
        String mapping = yaml(
            "mappings:",
            "  documentoId: \"hl7:id/@extension\"",
            "  emissao: { xpath: \"hl7:effectiveTime/@value\", tipo: data }",
            "  paciente:",
            "    id: \"hl7:recordTarget/hl7:patientRole/hl7:id/@extension\"");
        return Hl7Handler.v3()
            .templateContent(V3TestResources.read(TEMPLATE_PATH))
            .withMappingContent(mapping)
            .schema(V3TestResources.url(SCHEMA_PATH))
            .schemaValidation(mode)
            .buildWriter();
    }

    private static Map<String, Object> dataWithRecordTarget() {
        Map<String, Object> paciente = new LinkedHashMap<>();
        paciente.put("id", "PAC-0600");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentoId", "DOC-0600");
        data.put("emissao", OffsetDateTime.of(2026, 11, 9, 14, 30, 0, 0, ZoneOffset.ofHours(-3)));
        data.put("paciente", paciente);
        return data;
    }
}
