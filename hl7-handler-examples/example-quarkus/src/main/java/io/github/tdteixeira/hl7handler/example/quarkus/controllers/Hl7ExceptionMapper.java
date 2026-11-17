package io.github.tdteixeira.hl7handler.example.quarkus.controllers;

import module java.base;

import io.github.tdteixeira.hl7handler.Hl7Exception;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class Hl7ExceptionMapper implements ExceptionMapper<Hl7Exception> {

    @Override
    public Response toResponse(final Hl7Exception exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}
