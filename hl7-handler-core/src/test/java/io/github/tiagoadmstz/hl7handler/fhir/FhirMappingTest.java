package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FhirMappingTest {

    public record TestPatientVO(String prontuario, String sobrenome, String nome, String sexo, String nascimento) {
    }

    @Test
    @DisplayName("Deve mapear Patient R4 para Map e fazer bindTo para VO")
    void shouldMapPatientR4ToVo() throws IOException {
        String yaml = """
            familia: fhir
            recurso: Patient
            mappings:
              prontuario: "identifier[0].value"
              sobrenome: "name[0].family"
              nome: "name[0].given[0]"
              sexo: "gender"
              nascimento: "birthDate"
            """;

        String json = loadSample("samples/fhir/r4/patient-r4.json");
        FhirProcessor processor = Hl7Handler.fhir()
            .withMapping(yaml)
            .build();

        FhirResult result = processor.read(json);
        Map<String, Object> map = result.toMap();

        assertThat(map.get("prontuario")).isEqualTo("123456");
        assertThat(map.get("sobrenome")).isEqualTo("Silva");
        assertThat(map.get("nome")).isEqualTo("Maria");
        assertThat(map.get("sexo")).isEqualTo("female");
        assertThat(map.get("nascimento")).isEqualTo("1980-05-15");

        TestPatientVO vo = result.bindTo(TestPatientVO.class);
        assertThat(vo.prontuario()).isEqualTo("123456");
        assertThat(vo.sobrenome()).isEqualTo("Silva");
        assertThat(vo.nome()).isEqualTo("Maria");
    }

    @Test
    @DisplayName("Deve mapear Observation R4 com diretivas de tipo, constante e fallback")
    void shouldMapObservationWithDirectives() throws IOException {
        String yaml = """
            familia: fhir
            recurso: Observation
            mappings:
              id: "id"
              status: "status"
              sinalVital: { de: "code.coding[0].display", fallback: "code.text" }
              valor: { de: "valueQuantity.value", tipo: numero }
              unidade: { de: "valueQuantity.unit", padrao: "N/A" }
              origem: { valor: "MONITOR" }
            """;

        String json = loadSample("samples/fhir/r4/observation-r4.json");
        FhirProcessor processor = Hl7Handler.fhir()
            .withMapping(yaml)
            .build();

        Map<String, Object> map = processor.read(json).toMap();

        assertThat(map.get("id")).isEqualTo("obs-001");
        assertThat(map.get("status")).isEqualTo("final");
        assertThat(map.get("sinalVital")).isEqualTo("MDC_PULS_OXIM_SAT_O2");
        assertThat(map.get("valor")).isEqualTo(98.0);
        assertThat(map.get("unidade")).isEqualTo("%");
        assertThat(map.get("origem")).isEqualTo("MONITOR");
    }

    @Test
    @DisplayName("Deve aplicar sobreposição porVersao entre R4 e R5")
    void shouldApplyVersionOverrides() throws IOException {
        String yaml = """
            familia: fhir
            mappings:
              dispositivo: "device.reference"
            porVersao:
              R5:
                dispositivo: "device.reference.reference"
            """;

        String r4Json = loadSample("samples/fhir/r4/observation-r4.json");
        String r5Json = loadSample("samples/fhir/r5/observation-r5.json");

        FhirProcessor processor = Hl7Handler.fhir()
            .withMapping(yaml)
            .build();

        Map<String, Object> r4Map = processor.read(r4Json).toMap();
        Map<String, Object> r5Map = processor.read(r5Json).toMap();

        assertThat(r4Map.get("dispositivo")).isEqualTo("Device/dev-001");
        assertThat(r5Map.get("dispositivo")).isEqualTo("Device/dev-001");
    }

    @Test
    @DisplayName("Deve mapear lista de recursos dentro de um Bundle transaction")
    @SuppressWarnings("unchecked")
    void shouldMapBundleResourceList() throws IOException {
        String yaml = """
            familia: fhir
            recurso: Bundle
            mappings:
              tipo: "type"
              itens:
                lista: "entry[*].resource"
                item:
                  tipoRecurso: "resourceType"
                  id: "id"
            """;

        String json = loadSample("samples/fhir/r4/bundle-transaction-r4.json");
        FhirProcessor processor = Hl7Handler.fhir()
            .withMapping(yaml)
            .build();

        Map<String, Object> map = processor.read(json).toMap();

        assertThat(map.get("tipo")).isEqualTo("transaction");
        List<Map<String, Object>> itens = (List<Map<String, Object>>) map.get("itens");
        assertThat(itens).hasSize(3);
        assertThat(itens.get(0).get("tipoRecurso")).isEqualTo("Patient");
        assertThat(itens.get(1).get("tipoRecurso")).isEqualTo("Device");
        assertThat(itens.get(2).get("tipoRecurso")).isEqualTo("Observation");
    }

    private String loadSample(String path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException("Sample resource not found: " + path);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        stream.close();
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
