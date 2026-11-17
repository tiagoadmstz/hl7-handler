package io.github.tdteixeira.hl7handler.example.spring.controllers;

import io.github.tdteixeira.hl7handler.example.stub.model.PacienteVO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.tdteixeira.hl7handler.example.spring.services.HL7SpringService;
import io.github.tdteixeira.hl7handler.example.spring.services.LaboryService;
import io.github.tdteixeira.hl7handler.example.spring.services.PatientService;

@RestController
@RequestMapping("/api/hl7")
public class Hl7SpringController {

    private final PatientService patientService;
    private final LaboryService laboryService;
    private final HL7SpringService hl7SpringService;

    public Hl7SpringController(
        PatientService patientService,
        LaboryService laboryService,
        HL7SpringService hl7SpringService
    ) {
        this.patientService = patientService;
        this.laboryService = laboryService;
        this.hl7SpringService = hl7SpringService;
    }

    @GetMapping(value = "/sample-patient", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSamplePatientJson() {
        try {
            String json = patientService.getPatientMessage();
            return ResponseEntity.ok().body(json);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(exception.getMessage()));
        }
    }

    @GetMapping(value = "/sample-lab", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSampleLabJson() {
        try {
            String json = laboryService.getLaboratorioMessage();
            return ResponseEntity.ok().body(json);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(exception.getMessage()));
        }
    }

    @PostMapping(value = "/patient-vo", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PacienteVO> parseToPatientVo(@RequestBody String rawHl7) {
        return ResponseEntity.ok(patientService.parseToPatientVo(rawHl7));
    }

    @PostMapping(value = "/ack", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateAck(@RequestBody String rawHl7) {
        return ResponseEntity.ok(hl7SpringService.generateAck(rawHl7));
    }
}
