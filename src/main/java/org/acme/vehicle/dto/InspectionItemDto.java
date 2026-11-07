package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class InspectionItemDto {
    @JsonProperty("point_code")
    public String pointCode;

    public String condition;

    public String note;

    @JsonProperty("photo_refs")
    public List<String> photoRefs = new ArrayList<>();

    public InspectionItemDto() {}

    public InspectionItemDto(String pointCode, String condition, String note, List<String> photoRefs) {
        this.pointCode = pointCode;
        this.condition = condition;
        this.note = note;
        this.photoRefs = photoRefs != null ? photoRefs : new ArrayList<>();
    }
}

