package io.github.tdteixeira.hl7handler.example.quarkus.controllers;

import io.github.tdteixeira.hl7handler.example.quarkus.services.LaboryService;
import io.github.tdteixeira.hl7handler.example.quarkus.services.PatientService;
import io.github.tdteixeira.hl7handler.example.stub.model.PacienteVO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/hl7")
@ApplicationScoped
public class Hl7QuarkusController {

    private final PatientService patientService;
    private final LaboryService laboryService;

    public Hl7QuarkusController(
            final PatientService patientService,
            final LaboryService laboryService
    ) {
        this.patientService = patientService;
        this.laboryService = laboryService;
    }

    @GET
    @Path("/sample-patient")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSamplePatientJson() {
        try {
            String json = patientService.getPatientMessage();
            return Response.ok(json).build();
        } catch (Exception exception) {
            return Response.serverError()
                    .entity("{\"error\": \"" + exception.getMessage() + "\"}")
                    .build();
        }
    }

    @GET
    @Path("/sample-lab")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSampleLabJson() {
        try {
            String json = laboryService.getLaboratorioMessage();
            return Response.ok(json).build();
        } catch (Exception exception) {
            return Response.serverError()
                    .entity("{\"error\": \"" + exception.getMessage() + "\"}")
                    .build();
        }
    }

    @POST
    @Path("/patient-vo")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response parseToPatientVo(final String rawHl7) {
        if (rawHl7 == null || rawHl7.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"HL7 payload cannot be empty\"}")
                    .build();
        }

        try {
            PacienteVO vo = patientService.parseToPatientVo(rawHl7);
            return Response.ok(vo).build();
        } catch (Exception exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"" + exception.getMessage() + "\"}")
                    .build();
        }
    }
}
