package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.Hl7ParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FhirProcessorTest {

    @Test
    @DisplayName("Deve ler recurso Patient R4 e extrair campos básicos")
    void shouldReadPatientR4() throws IOException {
        String json = loadSample("samples/fhir/r4/patient-r4.json");
        FhirProcessor processor = Hl7Handler.fhir().build();

        FhirResult result = processor.read(json);
        FhirResource patient = result.resource();

        assertThat(patient.resourceType()).isEqualTo("Patient");
        assertThat(patient.id()).isEqualTo("pat-001");
        assertThat(patient.versionId()).isEqualTo("1");
        assertThat(patient.version()).isEqualTo(FhirVersion.R4);
        assertThat(patient.get("name[0].family")).isEqualTo("Silva");
        assertThat(patient.get("name[0].given[0]")).isEqualTo("Maria");
        assertThat(patient.get("gender")).isEqualTo("female");
        assertThat(patient.get("birthDate")).isEqualTo("1980-05-15");
        assertThat(patient.isBundle()).isFalse();
    }

    @Test
    @DisplayName("Deve ler recurso Observation R4 com unidade e código")
    void shouldReadObservationR4() throws IOException {
        String json = loadSample("samples/fhir/r4/observation-r4.json");
        FhirProcessor processor = Hl7Handler.fhir().build();

        FhirResource obs = processor.read(json).resource();

        assertThat(obs.resourceType()).isEqualTo("Observation");
        assertThat(obs.id()).isEqualTo("obs-001");
        assertThat(obs.get("status")).isEqualTo("final");
        assertThat(obs.get("code.coding[0].code")).isEqualTo("150020");
        assertThat(obs.get("valueQuantity.value")).isEqualTo("98.0");
        assertThat(obs.get("valueQuantity.unit")).isEqualTo("%");
        assertThat(obs.get("subject.reference")).isEqualTo("Patient/pat-001");
        assertThat(obs.get("device.reference")).isEqualTo("Device/dev-001");
    }

    @Test
    @DisplayName("Deve ler recurso Device R4 com identificador EUI-64")
    void shouldReadDeviceR4() throws IOException {
        String json = loadSample("samples/fhir/r4/device-r4.json");
        FhirProcessor processor = Hl7Handler.fhir().build();

        FhirResource dev = processor.read(json).resource();

        assertThat(dev.resourceType()).isEqualTo("Device");
        assertThat(dev.id()).isEqualTo("dev-001");
        assertThat(dev.get("identifier[0].value")).isEqualTo("00-A0-C9-14-C8-29-00-00");
        assertThat(dev.get("status")).isEqualTo("active");
    }

    @Test
    @DisplayName("Deve ler Bundle transaction R4 e listar recursos com getAll")
    void shouldReadBundleTransactionR4() throws IOException {
        String json = loadSample("samples/fhir/r4/bundle-transaction-r4.json");
        FhirProcessor processor = Hl7Handler.fhir().build();

        FhirResource bundle = processor.read(json).resource();

        assertThat(bundle.isBundle()).isTrue();
        assertThat(bundle.get("type")).isEqualTo("transaction");
        List<String> types = bundle.getAll("entry[*].resource.resourceType");
        assertThat(types).containsExactly("Patient", "Device", "Observation");
    }

    @Test
    @DisplayName("Deve detectar versão R5 em Observation R5")
    void shouldDetectVersionR5() throws IOException {
        String json = loadSample("samples/fhir/r5/observation-r5.json");
        FhirProcessor processor = Hl7Handler.fhir().build();

        FhirResource obs = processor.read(json).resource();

        assertThat(obs.version()).isEqualTo(FhirVersion.R5);
        assertThat(obs.get("code.coding[0].code")).isEqualTo("150020");
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException ao ler JSON vazio")
    void shouldThrowOnEmptyJson() {
        FhirProcessor processor = Hl7Handler.fhir().build();

        assertThatThrownBy(() -> processor.read(""))
            .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException ao ler JSON só com espaços")
    void shouldThrowOnBlankJson() {
        FhirProcessor processor = Hl7Handler.fhir().build();

        assertThatThrownBy(() -> processor.read("   "))
            .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException quando falta resourceType")
    void shouldThrowOnMissingResourceType() {
        FhirProcessor processor = Hl7Handler.fhir().build();

        assertThatThrownBy(() -> processor.read("{\"foo\":\"bar\"}"))
            .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException ao ler JSON malformado")
    void shouldThrowOnMalformedJson() {
        FhirProcessor processor = Hl7Handler.fhir().build();

        assertThatThrownBy(() -> processor.read("not-json"))
            .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException quando o JSON não é um objeto")
    void shouldThrowOnJsonArray() {
        FhirProcessor processor = Hl7Handler.fhir().build();

        assertThatThrownBy(() -> processor.read("[]"))
            .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve lançar Hl7ParseException ao ler bytes vazios")
    void shouldThrowOnEmptyBytes() {
        FhirProcessor processor = Hl7Handler.fhir().build();

        assertThatThrownBy(() -> processor.readBytes(new byte[0]))
            .isInstanceOf(Hl7ParseException.class);
    }

    @Test
    @DisplayName("Deve converter unidades IEEE 11073-10101 (MDC) para UCUM")
    void shouldConvertMdcToUcum() {
        assertThat(FhirUnits.toUcum("MDC_DIM_PERCENT")).isEqualTo("%");
        assertThat(FhirUnits.toUcum("262688")).isEqualTo("%");
        assertThat(FhirUnits.toUcum("MDC_DIM_BEAT_PER_MIN")).isEqualTo("/min");
        assertThat(FhirUnits.toUcum("MDC_DIM_MMHG")).isEqualTo("mm[Hg]");
        assertThat(FhirUnits.toUcum("MDC_DIM_DEGC")).isEqualTo("Cel");
        assertThat(FhirUnits.toUcum("MDC_DIM_FAHR")).isEqualTo("[degF]");
        assertThat(FhirUnits.toUcum("CODIGO_DESCONHECIDO")).isNull();
        assertThat(FhirUnits.hasUcumEquivalence("MDC_DIM_PERCENT")).isTrue();
        assertThat(FhirUnits.hasUcumEquivalence("CODIGO_DESCONHECIDO")).isFalse();
    }

    @Test
    @DisplayName("Deve formatar e serializar JSON em toRawJson")
    void shouldSerializeRawJson() throws IOException {
        String json = loadSample("samples/fhir/r4/patient-r4.json");
        FhirProcessor processor = Hl7Handler.fhir().build();

        FhirResult result = processor.read(json);

        assertThat(result.toRawJson()).contains("\"resourceType\" : \"Patient\"");
        assertThat(result.toJson()).contains("\"resourceType\" : \"Patient\"");
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
