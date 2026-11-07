package org.acme.vehicle.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.acme.vehicle.dto.ApiErrorResponse;
import org.acme.vehicle.dto.FieldErrorDetail;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@Provider
public class CentralizedExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = Logger.getLogger(CentralizedExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof ValidationException ve) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiErrorResponse(ve.getMessage(), ve.getErrors()))
                    .build();
        }

        if (exception instanceof NotFoundException nfe) {
            return Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiErrorResponse(nfe.getMessage(), new ArrayList<>()))
                    .build();
        }

        if (exception instanceof ConflictException ce) {
            return Response.status(Response.Status.CONFLICT)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiErrorResponse(ce.getMessage(), ce.getErrors()))
                    .build();
        }

        if (exception instanceof ConstraintViolationException cve) {
            List<FieldErrorDetail> errors = new ArrayList<>();
            for (ConstraintViolation<?> violation : cve.getConstraintViolations()) {
                String field = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "unknown";
                errors.add(new FieldErrorDetail(field, violation.getMessage()));
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiErrorResponse("Validation error", errors))
                    .build();
        }

        if (exception instanceof WebApplicationException wae) {
            int status = wae.getResponse().getStatus();
            return Response.status(status)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ApiErrorResponse(wae.getMessage(), new ArrayList<>()))
                    .build();
        }

        LOG.error("Unhandled exception caught: ", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiErrorResponse("Internal server error: " + exception.getMessage(), new ArrayList<>()))
                .build();
    }
}

