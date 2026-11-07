package org.acme.vehicle.service;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.acme.vehicle.dto.*;
import org.acme.vehicle.entity.Inspection;
import org.acme.vehicle.entity.InspectionItem;
import org.acme.vehicle.entity.Vehicle;
import org.acme.vehicle.exception.ConflictException;
import org.acme.vehicle.exception.NotFoundException;
import org.acme.vehicle.exception.ValidationException;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class InspectionService {

    @Inject
    ChecklistService checklistService;

    @Inject
    AuditService auditService;

    public List<VehicleDto> getAllVehicles() {
        List<Vehicle> list = Vehicle.listAll(Sort.by("id").ascending());
        List<VehicleDto> result = new ArrayList<>();
        for (Vehicle v : list) {
            result.add(new VehicleDto(v.id, v.plateNumber, v.type, v.brand, v.model, v.year));
        }
        return result;
    }

    public Vehicle getVehicleOrThrow(Long vehicleId) {
        if (vehicleId == null) {
            throw new ValidationException("vehicle_id is required",
                    List.of(new FieldErrorDetail("vehicle_id", "vehicle_id is required")));
        }
        Vehicle vehicle = Vehicle.findById(vehicleId);
        if (vehicle == null) {
            throw new NotFoundException("Vehicle not found: " + vehicleId);
        }
        return vehicle;
    }

    public void validateItems(Long vehicleId, Boolean hasKey, List<InspectionItemDto> items, String targetStatus) {
        // R1: vehicle_id exists
        getVehicleOrThrow(vehicleId);

        List<FieldErrorDetail> errors = new ArrayList<>();
        boolean keyAvailable = hasKey != null ? hasKey : true;

        Set<String> presentPoints = new HashSet<>();

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                InspectionItemDto item = items.get(i);
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
        if ("SUBMITTED".equals(targetStatus)) {
            Set<String> allPoints = checklistService.getAllCarPointCodes();
            for (String code : allPoints) {
                if (!presentPoints.contains(code)) {
                    errors.add(new FieldErrorDetail("items." + code, "Point " + code + " is required for SUBMITTED status"));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed", errors);
        }
    }

    @Transactional
    public Inspection createDraft(CreateInspectionRequest request) {
        validateItems(request.vehicleId, request.hasKey, request.items, "DRAFT");

        Inspection inspection = new Inspection();
        inspection.vehicleId = request.vehicleId;
        inspection.inspectorName = request.inspectorName != null ? request.inspectorName : "Unknown";
        inspection.hasKey = request.hasKey != null ? request.hasKey : true;
        inspection.status = "DRAFT";
        inspection.version = 1;
        inspection.createdAt = Instant.now();
        inspection.updatedAt = Instant.now();

        if (request.items != null) {
            for (InspectionItemDto itemDto : request.items) {
                InspectionItem item = new InspectionItem();
                item.inspection = inspection;
                item.pointCode = itemDto.pointCode;
                item.condition = itemDto.condition;
                item.note = itemDto.note;
                item.photoRefs = itemDto.photoRefs != null ? itemDto.photoRefs : new ArrayList<>();
                inspection.items.add(item);
            }
        }

        inspection.persist();

        auditService.recordAudit(inspection.id, "CREATE_INSPECTION", null, "DRAFT",
                inspection.inspectorName, Map.of("version", 1));

        return inspection;
    }

    @Transactional
    public Inspection updateInspection(Long id, UpdateInspectionRequest request) {
        Inspection inspection = Inspection.findById(id);
        if (inspection == null) {
            throw new NotFoundException("Inspection not found: " + id);
        }

        // R6: Hanya DRAFT dan RECHECK yang dapat diubah lewat PUT
        if (!"DRAFT".equals(inspection.status) && !"RECHECK".equals(inspection.status)) {
            throw new ConflictException("Cannot update inspection in status: " + inspection.status);
        }

        // R6: Version check
        if (request.version == null || !request.version.equals(inspection.version)) {
            throw new ConflictException("Stale version conflict. Expected: " + inspection.version + ", got: " + request.version);
        }

        // Validate items
        validateItems(inspection.vehicleId, request.hasKey, request.items, inspection.status);

        if (request.inspectorName != null) {
            inspection.inspectorName = request.inspectorName;
        }
        if (request.hasKey != null) {
            inspection.hasKey = request.hasKey;
        }

        // Replace all items
        inspection.items.clear();
        if (request.items != null) {
            for (InspectionItemDto itemDto : request.items) {
                InspectionItem item = new InspectionItem();
                item.inspection = inspection;
                item.pointCode = itemDto.pointCode;
                item.condition = itemDto.condition;
                item.note = itemDto.note;
                item.photoRefs = itemDto.photoRefs != null ? itemDto.photoRefs : new ArrayList<>();
                inspection.items.add(item);
            }
        }

        inspection.version = inspection.version + 1;
        inspection.updatedAt = Instant.now();

        auditService.recordAudit(inspection.id, "UPDATE_INSPECTION", inspection.status, inspection.status,
                inspection.inspectorName, Map.of("version", inspection.version));

        return inspection;
    }

    public Inspection getInspectionOrThrow(Long id) {
        Inspection inspection = Inspection.findById(id);
        if (inspection == null) {
            throw new NotFoundException("Inspection not found: " + id);
        }
        return inspection;
    }

    public Map<String, Object> listInspections(String status, Long vehicleId, int page, int limit) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.min(Math.max(limit, 1), 100);

        StringBuilder query = new StringBuilder("1 = 1");
        Parameters params = new Parameters();

        if (status != null && !status.trim().isEmpty()) {
            query.append(" AND status = :status");
            params.and("status", status.trim());
        }
        if (vehicleId != null) {
            query.append(" AND vehicleId = :vehicleId");
            params.and("vehicleId", vehicleId);
        }

        PanacheQuery<Inspection> panacheQuery = Inspection.find(query.toString(),
                Sort.by("updatedAt").descending(), params);

        long total = panacheQuery.count();
        List<Inspection> list = panacheQuery.page(pageIndex, pageSize).list();

        List<InspectionDetailDto> dtoList = new ArrayList<>();
        for (Inspection ins : list) {
            dtoList.add(toDetailDto(ins));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", dtoList);
        result.put("meta", new PageMeta(pageIndex + 1, pageSize, total));
        return result;
    }

    @Transactional
    public Inspection transitionStatus(Long id, TransitionRequest request) {
        Inspection inspection = Inspection.findById(id);
        if (inspection == null) {
            throw new NotFoundException("Inspection not found: " + id);
        }

        String targetStatus = request.to;
        if (targetStatus == null || !checklistService.isValidStatus(targetStatus)) {
            throw new ValidationException("Invalid status transition target: " + targetStatus);
        }

        String currentStatus = inspection.status;

        // R4: Valid transitions:
        // DRAFT -> SUBMITTED
        // SUBMITTED -> APPROVED
        // SUBMITTED -> RECHECK
        // RECHECK -> SUBMITTED
        boolean valid = false;
        if ("DRAFT".equals(currentStatus) && "SUBMITTED".equals(targetStatus)) {
            valid = true;
        } else if ("SUBMITTED".equals(currentStatus) && "APPROVED".equals(targetStatus)) {
            valid = true;
        } else if ("SUBMITTED".equals(currentStatus) && "RECHECK".equals(targetStatus)) {
            valid = true;
        } else if ("RECHECK".equals(currentStatus) && "SUBMITTED".equals(targetStatus)) {
            valid = true;
        }

        if (!valid) {
            throw new ConflictException("Illegal transition",
                    List.of(new FieldErrorDetail("to", currentStatus + " cannot move to " + targetStatus)));
        }

        // R5: Transition to SUBMITTED requires all 14 points
        if ("SUBMITTED".equals(targetStatus)) {
            List<InspectionItemDto> itemDtos = new ArrayList<>();
            for (InspectionItem item : inspection.items) {
                itemDtos.add(new InspectionItemDto(item.pointCode, item.condition, item.note, item.photoRefs));
            }
            validateItems(inspection.vehicleId, inspection.hasKey, itemDtos, "SUBMITTED");
        }

        inspection.status = targetStatus;
        inspection.version = inspection.version + 1;
        inspection.updatedAt = Instant.now();

        Map<String, Object> meta = new HashMap<>();
        meta.put("version", inspection.version);
        if (request.note != null) {
            meta.put("note", request.note);
        }

        auditService.recordAudit(inspection.id, "STATUS_TRANSITION", currentStatus, targetStatus,
                request.actor, meta);

        return inspection;
    }

    public InspectionDetailDto toDetailDto(Inspection ins) {
        InspectionDetailDto dto = new InspectionDetailDto();
        dto.id = ins.id;
        dto.vehicleId = ins.vehicleId;
        dto.clientId = ins.clientId;
        dto.inspectorName = ins.inspectorName;
        dto.hasKey = ins.hasKey;
        dto.status = ins.status;
        dto.version = ins.version;
        dto.createdAt = ins.createdAt;
        dto.updatedAt = ins.updatedAt;

        if (ins.items != null) {
            for (InspectionItem item : ins.items) {
                dto.items.add(new InspectionItemDto(item.pointCode, item.condition, item.note, item.photoRefs));
            }
        }
        return dto;
    }
}

