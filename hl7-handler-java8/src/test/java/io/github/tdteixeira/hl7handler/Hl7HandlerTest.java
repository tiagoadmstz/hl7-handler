package io.github.tdteixeira.hl7handler;

import io.github.tdteixeira.hl7handler.model.Hl7Message;
import io.github.tdteixeira.hl7handler.model.Hl7Version;
import io.github.tdteixeira.hl7handler.network.MllpStreamReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class Hl7HandlerTest {

    private String sampleHl7;
    private InputStream mappingYamlStream;

    @BeforeEach
    void setUp() {
        sampleHl7 = TestResources.read("/samples/sample-v25.hl7");
        mappingYamlStream = getClass().getResourceAsStream("/mappings/paciente-mapping.yaml");
        assertThat(mappingYamlStream).isNotNull();
    }

    @Test
    @DisplayName("Should detect HL7 version 2.5 automatically from MSH-12")
    void shouldDetectVersionAutomatically() {
        Hl7Version version = Hl7Handler.detectVersion(sampleHl7);
        assertThat(version).isEqualTo(Hl7Version.V2_5);
        assertThat(version.getIdentifier()).isEqualTo("2.5");
    }

    @Test
    @DisplayName("Should parse HL7 message structure correctly")
    void shouldParseMessageStructure() {
        Hl7Message message = Hl7Handler.reader()
                .from(sampleHl7)
                .toMessage();

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_5);
        assertThat(message.getSegments()).hasSize(5);

        // Path navigation
        assertThat(message.get("MSH.9.1")).isEqualTo("ADT");
        assertThat(message.get("MSH.9.2")).isEqualTo("A01");
        assertThat(message.get("MSH.10")).isEqualTo("MSG202611090001");
        assertThat(message.get("PID.3.1")).isEqualTo("123456");
        assertThat(message.get("PID.5.1")).isEqualTo("SILVA");
        assertThat(message.get("PID.5.2")).isEqualTo("JOAO");
        assertThat(message.get("PID.11.3")).isEqualTo("SAO PAULO");
        assertThat(message.get("PID.11.4")).isEqualTo("SP");
        assertThat(message.get("PV1.3.1")).isEqualTo("CLINICA");
        assertThat(message.get("PV1.3.2")).isEqualTo("101");
    }

    @Test
    @DisplayName("Should translate HL7 into semantic JSON using external YAML mapping")
    void shouldTranslateToJsonUsingYamlMapping() {
        String json = Hl7Handler.reader()
                .from(sampleHl7)
                .withMapping(mappingYamlStream)
                .toJson();

        assertThat(json)
                .contains("\"mensagemId\" : \"MSG202611090001\"")
                .contains("\"tipoMensagem\" : \"ADT\"")
                .contains("\"evento\" : \"A01\"")
                .contains("\"prontuario\" : \"123456\"")
                .contains("\"nome\" : \"JOAO\"")
                .contains("\"sobrenome\" : \"SILVA\"")
                .contains("\"cidade\" : \"SAO PAULO\"")
                .contains("\"uf\" : \"SP\"")
                .contains("\"leito\" : \"101\"");
    }

    @Test
    @DisplayName("Should bind HL7 directly into customer Value Object (VO)")
    void shouldBindToCustomerValueObject() {
        PacienteAdmissaoVO vo = Hl7Handler.reader()
                .from(sampleHl7)
                .withMapping(mappingYamlStream)
                .bindTo(PacienteAdmissaoVO.class);

        assertThat(vo).isNotNull();
        assertThat(vo.getMensagemId()).isEqualTo("MSG202611090001");
        assertThat(vo.getTipoMensagem()).isEqualTo("ADT");
        assertThat(vo.getEvento()).isEqualTo("A01");
        assertThat(vo.getVersaoHl7()).isEqualTo("2.5");

        assertThat(vo.getPaciente()).isNotNull();
        assertThat(vo.getPaciente().getProntuario()).isEqualTo("123456");
        assertThat(vo.getPaciente().getNome()).isEqualTo("JOAO");
        assertThat(vo.getPaciente().getSobrenome()).isEqualTo("SILVA");
        assertThat(vo.getPaciente().getDataNascimento()).isEqualTo("19850515");
        assertThat(vo.getPaciente().getSexo()).isEqualTo("M");

        assertThat(vo.getPaciente().getEndereco()).isNotNull();
        assertThat(vo.getPaciente().getEndereco().getCidade()).isEqualTo("SAO PAULO");
        assertThat(vo.getPaciente().getEndereco().getUf()).isEqualTo("SP");
        assertThat(vo.getPaciente().getEndereco().getCep()).isEqualTo("01001-000");

        assertThat(vo.getInternacao()).isNotNull();
        assertThat(vo.getInternacao().getLeito()).isEqualTo("101");
        assertThat(vo.getInternacao().getQuarto()).isEqualTo("CLINICA");
        assertThat(vo.getInternacao().getMedico()).isEqualTo("MEDICO");
    }

    @Test
    @DisplayName("Should read from MLLP network byte stream and parse correctly")
    void shouldReadFromMllpByteStream() {
        // Wrap sample HL7 into MLLP frame: <VT> payload <FS><CR>
        byte[] mllpFrame = Hl7Handler.network().wrap(sampleHl7);
        assertThat(mllpFrame[0]).isEqualTo(MllpStreamReader.START_BLOCK);
        assertThat(mllpFrame[mllpFrame.length - 2]).isEqualTo(MllpStreamReader.END_BLOCK);
        assertThat(mllpFrame[mllpFrame.length - 1]).isEqualTo(MllpStreamReader.TRAILER);

        // Read through MLLP stream
        ByteArrayInputStream networkStream = new ByteArrayInputStream(mllpFrame);
        Hl7Message message = Hl7Handler.reader()
                .fromMllp(networkStream)
                .toMessage();

        assertThat(message.getVersion()).isEqualTo(Hl7Version.V2_5);
        assertThat(message.get("PID.5.1")).isEqualTo("SILVA");
    }

    @Test
    @DisplayName("Should format and write HL7 message back to text")
    void shouldWriteHl7BackToText() {
        Hl7Message message = Hl7Handler.reader()
                .from(sampleHl7)
                .toMessage();

        String serialized = Hl7Handler.toHl7(message);
        assertThat(serialized).startsWith("MSH|^~\\&|");
        assertThat(serialized).contains("PID|1||123456^^^HOSPITAL^MR||SILVA^JOAO^CARLOS^^DR");
    }
}
