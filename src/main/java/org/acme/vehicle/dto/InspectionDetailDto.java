package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InspectionDetailDto {
    public Long id;

    @JsonProperty("vehicle_id")
    public Long vehicleId;

    @JsonProperty("client_id")
    public UUID clientId;

    @JsonProperty("inspector_name")
    public String inspectorName;

    @JsonProperty("has_key")
    public Boolean hasKey;

    public String status;

    public Integer version;

    @JsonProperty("created_at")
    public Instant createdAt;

    @JsonProperty("updated_at")
    public Instant updatedAt;

    public List<InspectionItemDto> items = new ArrayList<>();
}

