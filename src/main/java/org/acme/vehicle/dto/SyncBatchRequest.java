package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SyncBatchRequest {
    @JsonProperty("device_id")
    public String deviceId;

    public List<SyncItemRequest> inspections;
}

