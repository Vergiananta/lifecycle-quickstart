package org.acme.vehicle.exception;

import org.acme.vehicle.dto.FieldErrorDetail;
import java.util.ArrayList;
import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<FieldErrorDetail> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }

    public ValidationException(String message, List<FieldErrorDetail> errors) {
        super(message);
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public List<FieldErrorDetail> getErrors() {
        return errors;
    }
}

