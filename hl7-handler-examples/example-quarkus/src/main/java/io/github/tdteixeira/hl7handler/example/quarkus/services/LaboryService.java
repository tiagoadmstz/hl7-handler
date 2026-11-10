package io.github.tdteixeira.hl7handler.example.quarkus.services;

import io.github.tdteixeira.hl7handler.example.stub.Hl7MockData;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LaboryService {

    private static final String LAB_MAPPING_FILE = "/mappings/laboratorio-stub-mapping.yaml";

    private final HL7QuarkusService hl7QuarkusService;

    public LaboryService(final HL7QuarkusService hl7QuarkusService) {
        this.hl7QuarkusService = hl7QuarkusService;
    }

    public String getLaboratorioMessage() {
        return hl7QuarkusService.processHL7Message(
                Hl7MockData.ORU_R01_LAB,
                LAB_MAPPING_FILE,
                String.class
        );
    }
}
