package io.github.tdteixeira.hl7handler.example.spring.services;

import org.springframework.stereotype.Service;

import io.github.tdteixeira.hl7handler.example.stub.Hl7MockData;

@Service
public class LaboryService {

    private static final String LAB_MAPPING_FILE = "/mappings/laboratorio-stub-mapping.yaml";

    private final HL7SpringService hl7SpringService;

    public LaboryService(HL7SpringService hl7SpringService) {
        this.hl7SpringService = hl7SpringService;
    }

    public String getLaboratorioMessage() {
        return hl7SpringService.processHL7Message(
            Hl7MockData.ORU_R01_LAB,
            LAB_MAPPING_FILE,
            String.class
        );
    }
}
