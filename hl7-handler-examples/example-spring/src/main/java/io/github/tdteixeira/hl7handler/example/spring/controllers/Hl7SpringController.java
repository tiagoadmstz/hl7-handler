package io.github.tdteixeira.hl7handler.example.spring.controllers;

import io.github.tdteixeira.hl7handler.example.stub.model.PacienteVO;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.github.tdteixeira.hl7handler.example.spring.services.LaboryService;
import io.github.tdteixeira.hl7handler.example.spring.services.PatientService;

@RestController
@RequestMapping("/api/hl7")
public class Hl7SpringController {

    private final PatientService patientService;
    private final LaboryService laboryService;

    public Hl7SpringController(
        PatientService patientService,
        LaboryService laboryService
    ) {
        this.patientService = patientService;
        this.laboryService = laboryService;
    }

    @GetMapping(value = "/sample-patient", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSamplePatientJson() {
        try {
            String json = patientService.getPatientMessage();
            return ResponseEntity.ok(json);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body("{\"error\": \"" + exception.getMessage() + "\"}");
        }
    }

    @GetMapping(value = "/sample-lab", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getSampleLabJson() {
        try {
            String json = laboryService.getLaboratorioMessage();
            return ResponseEntity.ok(json);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body("{\"error\": \"" + exception.getMessage() + "\"}");
        }
    }

    @PostMapping(value = "/patient-vo", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PacienteVO> parseToPatientVo(@RequestBody String rawHl7) {
        try {
            PacienteVO vo = patientService.parseToPatientVo(rawHl7);
            return ResponseEntity.ok(vo);
        } catch (Exception exception) {
            return ResponseEntity.badRequest().build();
        }
    }
}
