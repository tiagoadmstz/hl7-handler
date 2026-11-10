package io.github.tdteixeira.hl7handler.example.quarkus.services;

import io.github.tdteixeira.hl7handler.example.stub.Hl7MockData;
import io.github.tdteixeira.hl7handler.example.stub.model.PacienteVO;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PatientService {

    private static final String PATIENT_MAPPING_FILE = "/mappings/paciente-stub-mapping.yaml";

    private final HL7QuarkusService hl7QuarkusService;

    public PatientService(final HL7QuarkusService hl7QuarkusService) {
        this.hl7QuarkusService = hl7QuarkusService;
    }

    public String getPatientMessage() {
        return hl7QuarkusService.processHL7Message(
                Hl7MockData.ADT_A01_V25,
                PATIENT_MAPPING_FILE,
                String.class
        );
    }

    public PacienteVO parseToPatientVo(final String rawHl7) {
        return hl7QuarkusService.processHL7Message(
                rawHl7,
                PATIENT_MAPPING_FILE,
                PacienteVO.class
        );
    }
}
