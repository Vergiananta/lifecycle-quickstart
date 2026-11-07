package org.acme.vehicle.service;

import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.acme.vehicle.entity.AuditLog;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AuditService {
    private static final Logger LOG = Logger.getLogger(AuditService.class);

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void recordAudit(Long inspectionId, String action, String fromStatus, String toStatus, String actor, Map<String, Object> meta) {
        try {
            AuditLog log = new AuditLog();
            log.inspectionId = inspectionId;
            log.action = action;
            log.fromStatus = fromStatus;
            log.toStatus = toStatus;
            log.actor = actor != null ? actor : "system";
            log.at = Instant.now();
            log.meta = meta != null ? meta : Collections.emptyMap();
            log.persist();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to persist audit log for inspection %d", inspectionId);
        }
    }

    public List<AuditLog> getAuditLogs(Long inspectionId) {
        return AuditLog.find("inspectionId", Sort.by("at").ascending(), inspectionId).list();
    }
}
