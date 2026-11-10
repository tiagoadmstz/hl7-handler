package io.github.tdteixeira.hl7handler.example.spring.services;

import org.springframework.stereotype.Service;
import io.github.tdteixeira.hl7handler.example.stub.Hl7MockData;
import io.github.tdteixeira.hl7handler.example.stub.model.PacienteVO;

@Service
public class PatientService {

    private static final String PATIENT_MAPPING_FILE = "/mappings/paciente-stub-mapping.yaml";

    private final HL7SpringService hl7SpringService;

    public PatientService(HL7SpringService hl7SpringService) {
        this.hl7SpringService = hl7SpringService;
    }

    public String getPatientMessage() {
        return hl7SpringService.processHL7Message(
            Hl7MockData.ADT_A01_V25,
            PATIENT_MAPPING_FILE,
            String.class
        );
    }

    public PacienteVO parseToPatientVo(final String rawHl7) {
        return hl7SpringService.processHL7Message(
            rawHl7,
            PATIENT_MAPPING_FILE,
            PacienteVO.class
        );
    }
}
