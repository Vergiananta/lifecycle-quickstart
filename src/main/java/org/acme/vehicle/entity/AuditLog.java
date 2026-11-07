package org.acme.vehicle.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import java.time.Instant;
import java.util.Map;

@MongoEntity(collection = "audit_logs")
public class AuditLog extends PanacheMongoEntity {
    @JsonProperty("inspection_id")
    public Long inspectionId;

    public String action;

    @JsonProperty("from_status")
    public String fromStatus;

    @JsonProperty("to_status")
    public String toStatus;

    public String actor;

    public Instant at;

    public Map<String, Object> meta;
}

