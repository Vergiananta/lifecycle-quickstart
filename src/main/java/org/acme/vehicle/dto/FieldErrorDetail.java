package org.acme.vehicle.dto;

public class FieldErrorDetail {
    public String field;
    public String reason;

    public FieldErrorDetail() {}

    public FieldErrorDetail(String field, String reason) {
        this.field = field;
        this.reason = reason;
    }
}

