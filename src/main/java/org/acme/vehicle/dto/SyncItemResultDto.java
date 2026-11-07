package org.acme.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SyncItemResultDto {
    @JsonProperty("client_id")
    public UUID clientId;

    public String result; // CREATED | UPDATED | DUPLICATE | CONFLICT | REJECTED

    @JsonProperty("inspection_id")
    public Long inspectionId;

    @JsonProperty("server_version")
    public Integer serverVersion;

    @JsonProperty("server_status")
    public String serverStatus;

    public List<FieldErrorDetail> errors = new ArrayList<>();

    public SyncItemResultDto() {}

    public static SyncItemResultDto created(UUID clientId, Long inspectionId, Integer serverVersion) {
        SyncItemResultDto dto = new SyncItemResultDto();
        dto.clientId = clientId;
        dto.result = "CREATED";
        dto.inspectionId = inspectionId;
        dto.serverVersion = serverVersion;
        return dto;
    }

    public static SyncItemResultDto updated(UUID clientId, Long inspectionId, Integer serverVersion) {
        SyncItemResultDto dto = new SyncItemResultDto();
        dto.clientId = clientId;
        dto.result = "UPDATED";
        dto.inspectionId = inspectionId;
        dto.serverVersion = serverVersion;
        return dto;
    }

    public static SyncItemResultDto duplicate(UUID clientId, Long inspectionId, Integer serverVersion) {
        SyncItemResultDto dto = new SyncItemResultDto();
        dto.clientId = clientId;
        dto.result = "DUPLICATE";
        dto.inspectionId = inspectionId;
        dto.serverVersion = serverVersion;
        return dto;
    }

    public static SyncItemResultDto conflict(UUID clientId, Long inspectionId, Integer serverVersion, String serverStatus) {
        SyncItemResultDto dto = new SyncItemResultDto();
        dto.clientId = clientId;
        dto.result = "CONFLICT";
        dto.inspectionId = inspectionId;
        dto.serverVersion = serverVersion;
        dto.serverStatus = serverStatus;
        return dto;
    }

    public static SyncItemResultDto rejected(UUID clientId, List<FieldErrorDetail> errors) {
        SyncItemResultDto dto = new SyncItemResultDto();
        dto.clientId = clientId;
        dto.result = "REJECTED";
        dto.errors = errors != null ? errors : new ArrayList<>();
        return dto;
    }
}

