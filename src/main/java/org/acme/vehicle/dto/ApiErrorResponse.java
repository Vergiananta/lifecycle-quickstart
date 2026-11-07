package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    public boolean success = false;
    public String message;
    public List<FieldErrorDetail> errors;

    public ApiErrorResponse() {}

    public ApiErrorResponse(String message) {
        this.message = message;
        this.errors = new ArrayList<>();
    }

    public ApiErrorResponse(String message, List<FieldErrorDetail> errors) {
        this.message = message;
        this.errors = errors;
    }
}

