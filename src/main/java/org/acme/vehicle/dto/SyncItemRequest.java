package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SyncItemRequest {
    @JsonProperty("client_id")
    public UUID clientId;

    @JsonProperty("base_version")
    public Integer baseVersion;

    @JsonProperty("vehicle_id")
    public Long vehicleId;

    @JsonProperty("inspector_name")
    public String inspectorName;

    @JsonProperty("has_key")
    public Boolean hasKey = true;

    public String status; // DRAFT or SUBMITTED

    public List<InspectionItemDto> items = new ArrayList<>();
}

