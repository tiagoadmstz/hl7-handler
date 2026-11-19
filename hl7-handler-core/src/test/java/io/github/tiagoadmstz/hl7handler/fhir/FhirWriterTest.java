package io.github.tiagoadmstz.hl7handler.fhir;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.fhir.writer.FhirWriter;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FhirWriterTest {

    public record VitalSignVO(String status, String codigo, double valor, String unidade) {
    }

    @Test
    @DisplayName("Deve gerar recurso Observation FHIR a partir de Map")
    void shouldWriteObservationFromMap() {
        String yaml = """
            familia: fhir
            recurso: Observation
            montagem:
              status: "$.status"
              code.coding[0].code: "$.codigo"
              valueQuantity.value: { de: "$.valor", tipo: numero }
              valueQuantity.unit: "$.unidade"
            """;

        FhirWriter writer = Hl7Handler.fhir()
            .withMapping(yaml)
            .buildWriter();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "final");
        data.put("codigo", "150020");
        data.put("valor", 98.5);
        data.put("unidade", "%");

        String json = writer.write(data);

        assertThat(json).contains("\"resourceType\" : \"Observation\"");
        assertThat(json).contains("\"status\" : \"final\"");
        assertThat(json).contains("\"code\" : \"150020\"");
        assertThat(json).contains("\"value\" : 98.5");
        assertThat(json).contains("\"unit\" : \"%\"");
    }

    @Test
    @DisplayName("Deve gerar recurso Observation FHIR a partir de Value Object")
    void shouldWriteObservationFromVo() {
        String yaml = """
            familia: fhir
            recurso: Observation
            montagem:
              status: "$.status"
              code.coding[0].code: "$.codigo"
              valueQuantity.value: { de: "$.valor", tipo: numero }
              valueQuantity.unit: "$.unidade"
            """;

        FhirWriter writer = Hl7Handler.fhir()
            .withMapping(yaml)
            .buildWriter();

        VitalSignVO vo = new VitalSignVO("preliminary", "147842", 72.0, "/min");
        String json = writer.write(vo);

        assertThat(json).contains("\"status\" : \"preliminary\"");
        assertThat(json).contains("\"code\" : \"147842\"");
        assertThat(json).contains("\"value\" : 72.0");
        assertThat(json).contains("\"unit\" : \"/min\"");
    }

    @Test
    @DisplayName("Deve converter mensagem HL7 v2 ORU^R01 em FHIR Bundle com Patient, Device e Observations")
    void shouldConvertV2OruToFhirBundle() {
        String v2 = "MSH|^~\\&|MONITOR_01|ICU|||20261119100000||ORU^R01|MSG001|P|2.6\r"
            + "PID|1||12345^^^HOSP||SILVA^MARIA||19800515|F\r"
            + "OBX|1|NM|150020^MDC_PULS_OXIM_SAT_O2||98|MDC_DIM_PERCENT|||||F|||20261119100500||||00-A0-C9-14-C8-29-00-00\r"
            + "OBX|2|NM|147842^MDC_ECG_HEART_RATE||75|MDC_DIM_BEAT_PER_MIN|||||F|||20261119100500||||00-A0-C9-14-C8-29-00-00\r";

        String yaml = """
            familia: fhir
            origem: v2
            recurso: Bundle
            tipo: transaction
            """;

        Hl7Message message = Hl7Handler.reader().from(v2).toMessage();
        FhirResource bundle = Hl7Handler.fhir()
            .withMapping(yaml)
            .convert(message);

        assertThat(bundle.isBundle()).isTrue();
        assertThat(bundle.get("type")).isEqualTo("transaction");

        List<String> types = bundle.getAll("entry[*].resource.resourceType");
        assertThat(types).containsExactly("Patient", "Device", "Observation", "Observation");

        assertThat(bundle.get("entry[0].resource.identifier[0].value")).isEqualTo("12345");
        assertThat(bundle.get("entry[1].resource.identifier[0].value")).isEqualTo("00-A0-C9-14-C8-29-00-00");

        assertThat(bundle.get("entry[2].resource.code.coding[0].code")).isEqualTo("150020");
        assertThat(bundle.get("entry[2].resource.valueQuantity.value")).isEqualTo("98");
        assertThat(bundle.get("entry[2].resource.valueQuantity.unit")).isEqualTo("%");
        assertThat(bundle.get("entry[2].resource.subject.reference")).startsWith("urn:uuid:");
        assertThat(bundle.get("entry[2].resource.device.reference")).startsWith("urn:uuid:");

        assertThat(bundle.get("entry[3].resource.code.coding[0].code")).isEqualTo("147842");
        assertThat(bundle.get("entry[3].resource.valueQuantity.value")).isEqualTo("75");
        assertThat(bundle.get("entry[3].resource.valueQuantity.unit")).isEqualTo("/min");
    }
}
