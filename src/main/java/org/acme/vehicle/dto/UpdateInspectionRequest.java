package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class UpdateInspectionRequest {
    public Integer version;

    @JsonProperty("inspector_name")
    public String inspectorName;

    @JsonProperty("has_key")
    public Boolean hasKey = true;

    public List<InspectionItemDto> items = new ArrayList<>();
}

