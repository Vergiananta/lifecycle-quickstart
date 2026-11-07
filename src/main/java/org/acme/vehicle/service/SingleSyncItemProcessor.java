package org.acme.vehicle.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.vehicle.dto.FieldErrorDetail;
import org.acme.vehicle.dto.InspectionItemDto;
import org.acme.vehicle.dto.SyncItemRequest;
import org.acme.vehicle.dto.SyncItemResultDto;
import org.acme.vehicle.entity.Inspection;
import org.acme.vehicle.entity.InspectionItem;
import org.acme.vehicle.entity.Vehicle;
import org.acme.vehicle.exception.ValidationException;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class SingleSyncItemProcessor {

    @Inject
    ChecklistService checklistService;

    @Inject
    AuditService auditService;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public SyncItemResultDto process(SyncItemRequest req, String deviceId) {
        if (req.clientId == null) {
            return SyncItemResultDto.rejected(null,
                    List.of(new FieldErrorDetail("client_id", "client_id is required")));
        }

        Inspection existing = Inspection.find("clientId", req.clientId).firstResult();

        // 1. client_id belum ada: validasi (R1 sampai R5) lalu CREATED (version 1), atau REJECTED
        if (existing == null) {
            List<FieldErrorDetail> errors = validateSyncItem(req);
            if (!errors.isEmpty()) {
                return SyncItemResultDto.rejected(req.clientId, errors);
            }

            Inspection ins = new Inspection();
            ins.clientId = req.clientId;
            ins.vehicleId = req.vehicleId;
            ins.inspectorName = req.inspectorName != null ? req.inspectorName : "Unknown";
            ins.hasKey = req.hasKey != null ? req.hasKey : true;
            ins.status = req.status != null ? req.status : "DRAFT";
            ins.version = 1;
            ins.createdAt = Instant.now();
            ins.updatedAt = Instant.now();

            if (req.items != null) {
                for (InspectionItemDto itemDto : req.items) {
                    InspectionItem item = new InspectionItem();
                    item.inspection = ins;
                    item.pointCode = itemDto.pointCode;
                    item.condition = itemDto.condition;
                    item.note = itemDto.note;
                    item.photoRefs = itemDto.photoRefs != null ? itemDto.photoRefs : new ArrayList<>();
                    ins.items.add(item);
                }
            }

            ins.persist();

            Map<String, Object> meta = new HashMap<>();
            meta.put("device_id", deviceId);
            meta.put("version", 1);
            meta.put("sync_result", "CREATED");
            auditService.recordAudit(ins.id, "SYNC_CREATED", null, ins.status, ins.inspectorName, meta);

            return SyncItemResultDto.created(req.clientId, ins.id, 1);
        }

        // 2. client_id sudah ada dan payload identik dengan data server: DUPLICATE (tanpa perubahan & tanpa audit)
        if (isPayloadIdentical(existing, req)) {
            return SyncItemResultDto.duplicate(req.clientId, existing.id, existing.version);
        }

        // 3. Selain itu, bila status server APPROVED atau SUBMITTED, atau base_version tidak sama dengan version server: CONFLICT
        if ("APPROVED".equals(existing.status) || "SUBMITTED".equals(existing.status)
                || req.baseVersion == null || !req.baseVersion.equals(existing.version)) {
            return SyncItemResultDto.conflict(req.clientId, existing.id, existing.version, existing.status);
        }

        // 4. Selain itu (status server DRAFT atau RECHECK, base_version sama): validasi lalu UPDATED, atau REJECTED
        List<FieldErrorDetail> errors = validateSyncItem(req);
        if (!errors.isEmpty()) {
            return SyncItemResultDto.rejected(req.clientId, errors);
        }

        existing.vehicleId = req.vehicleId;
        if (req.inspectorName != null) {
            existing.inspectorName = req.inspectorName;
        }
        if (req.hasKey != null) {
            existing.hasKey = req.hasKey;
        }
        if (req.status != null) {
            existing.status = req.status;
        }

        existing.items.clear();
        if (req.items != null) {
            for (InspectionItemDto itemDto : req.items) {
                InspectionItem item = new InspectionItem();
                item.inspection = existing;
                item.pointCode = itemDto.pointCode;
                item.condition = itemDto.condition;
                item.note = itemDto.note;
                item.photoRefs = itemDto.photoRefs != null ? itemDto.photoRefs : new ArrayList<>();
                existing.items.add(item);
            }
        }

        existing.version = existing.version + 1;
        existing.updatedAt = Instant.now();

        Map<String, Object> meta = new HashMap<>();
        meta.put("device_id", deviceId);
        meta.put("version", existing.version);
        meta.put("sync_result", "UPDATED");
        auditService.recordAudit(existing.id, "SYNC_UPDATED", "DRAFT", existing.status, existing.inspectorName, meta);

        return SyncItemResultDto.updated(req.clientId, existing.id, existing.version);
    }

    private boolean isPayloadIdentical(Inspection server, SyncItemRequest req) {
        if (!Objects.equals(server.vehicleId, req.vehicleId)) return false;
        if (!Objects.equals(server.inspectorName, req.inspectorName)) return false;
        boolean reqHasKey = req.hasKey != null ? req.hasKey : true;
        if (!Objects.equals(server.hasKey, reqHasKey)) return false;
        if (!Objects.equals(server.status, req.status)) return false;

        List<InspectionItemDto> reqItems = req.items != null ? req.items : Collections.emptyList();
        List<InspectionItem> serverItems = server.items != null ? server.items : Collections.emptyList();

        if (serverItems.size() != reqItems.size()) return false;

        Map<String, InspectionItem> serverItemMap = new HashMap<>();
        for (InspectionItem si : serverItems) {
            serverItemMap.put(si.pointCode, si);
        }

        for (InspectionItemDto ri : reqItems) {
            InspectionItem si = serverItemMap.get(ri.pointCode);
            if (si == null) return false;
            if (!Objects.equals(si.condition, ri.condition)) return false;

            String sNote = si.note != null ? si.note : "";
            String rNote = ri.note != null ? ri.note : "";
            if (!Objects.equals(sNote, rNote)) return false;

            List<String> sPhotos = si.photoRefs != null ? si.photoRefs : Collections.emptyList();
            List<String> rPhotos = ri.photoRefs != null ? ri.photoRefs : Collections.emptyList();
            if (!Objects.equals(sPhotos, rPhotos)) return false;
        }

        return true;
    }

    private List<FieldErrorDetail> validateSyncItem(SyncItemRequest req) {
        List<FieldErrorDetail> errors = new ArrayList<>();

        if (req.vehicleId == null) {
            errors.add(new FieldErrorDetail("vehicle_id", "vehicle_id is required"));
        } else {
            Vehicle vehicle = Vehicle.findById(req.vehicleId);
            if (vehicle == null) {
                errors.add(new FieldErrorDetail("vehicle_id", "Vehicle not found: " + req.vehicleId));
            }
        }

        boolean keyAvailable = req.hasKey != null ? req.hasKey : true;
        Set<String> presentPoints = new HashSet<>();

        if (req.items != null) {
            for (int i = 0; i < req.items.size(); i++) {
                InspectionItemDto item = req.items.get(i);
                if (item == null) continue;

                String pointCode = item.pointCode;
                String condition = item.condition;

                // R1: point_code in checklist
                if (!checklistService.isValidPointCode(pointCode)) {
                    errors.add(new FieldErrorDetail("items[" + i + "].point_code", "Invalid point_code: " + pointCode));
                    continue;
                }
                presentPoints.add(pointCode);

                // R1: condition valid
                if (!checklistService.isValidCondition(condition)) {
                    errors.add(new FieldErrorDetail("items." + pointCode + ".condition", "Invalid condition: " + condition));
                }

                // R2: If has_key = false, ENGINE points must be NOT_INSPECTABLE
                if (!keyAvailable && checklistService.isEngineSection(pointCode)) {
                    if (!"NOT_INSPECTABLE".equals(condition)) {
                        errors.add(new FieldErrorDetail("items." + pointCode,
                                "All ENGINE section points must be NOT_INSPECTABLE when has_key is false"));
                    }
                }

                // R3: NOT_INSPECTABLE must have note
                if ("NOT_INSPECTABLE".equals(condition)) {
                    if (item.note == null || item.note.trim().isEmpty()) {
                        errors.add(new FieldErrorDetail("items." + pointCode + ".note",
                                "Note is required for NOT_INSPECTABLE condition"));
                    }
                }

                // R3: MAJOR_DAMAGE must have >= 1 photo_refs
                if ("MAJOR_DAMAGE".equals(condition)) {
                    if (item.photoRefs == null || item.photoRefs.isEmpty()) {
                        errors.add(new FieldErrorDetail("items." + pointCode + ".photo_refs",
                                "At least one photo reference is required for MAJOR_DAMAGE"));
                    }
                }
            }
        }

        // R5: Ke SUBMITTED hanya bila semua 14 titik terisi
        if ("SUBMITTED".equals(req.status)) {
            Set<String> allPoints = checklistService.getAllCarPointCodes();
            for (String code : allPoints) {
                if (!presentPoints.contains(code)) {
                    errors.add(new FieldErrorDetail("items." + code, "Point " + code + " is required for SUBMITTED status"));
                }
            }
        }

        return errors;
    }
}

