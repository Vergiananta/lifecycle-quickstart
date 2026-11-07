package org.acme.vehicle.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.vehicle.dto.*;
import org.acme.vehicle.entity.AuditLog;
import org.acme.vehicle.entity.Inspection;
import org.acme.vehicle.service.AuditService;
import org.acme.vehicle.service.InspectionService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Path("/api/v1/inspections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Inspections", description = "Operations related to vehicle inspections")
public class InspectionResource {

    @Inject
    InspectionService inspectionService;

    @Inject
    AuditService auditService;

    @Inject
    SyncService syncService;

    @POST
    @Path("/sync")
    @Operation(summary = "Batch offline synchronization", description = "Synchronizes multiple inspections from offline clients idempotently")
    public Response syncInspections(SyncBatchRequest request) {
        SyncBatchResponseDto responseDto = syncService.sync(request);
        return Response.ok(ApiResponse.ok("Sync batch processed successfully", responseDto)).build();
    }

    @POST
    @Operation(summary = "Create inspection draft", description = "Creates a new inspection in DRAFT status")
    public Response createInspection(CreateInspectionRequest request) {
        Inspection inspection = inspectionService.createDraft(request);
        InspectionDetailDto dto = inspectionService.toDetailDto(inspection);
        return Response.status(Response.Status.CREATED)
                .entity(ApiResponse.created("Inspection draft created successfully", dto))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Update inspection", description = "Updates DRAFT or RECHECK inspection. Requires matching version")
    public Response updateInspection(@PathParam("id") Long id, UpdateInspectionRequest request) {
        Inspection updated = inspectionService.updateInspection(id, request);
        InspectionDetailDto dto = inspectionService.toDetailDto(updated);
        return Response.ok(ApiResponse.ok("Inspection updated successfully", dto)).build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get inspection detail", description = "Retrieves inspection by ID with all inspection items")
    public Response getInspection(@PathParam("id") Long id) {
        Inspection inspection = inspectionService.getInspectionOrThrow(id);
        InspectionDetailDto dto = inspectionService.toDetailDto(inspection);
        return Response.ok(ApiResponse.ok("Inspection retrieved successfully", dto)).build();
    }

    @GET
    @Operation(summary = "List inspections", description = "Lists inspections with filters and pagination")
    public Response listInspections(
            @QueryParam("status") String status,
            @QueryParam("vehicle_id") Long vehicleId,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        Map<String, Object> result = inspectionService.listInspections(status, vehicleId, page, limit);
        @SuppressWarnings("unchecked")
        List<InspectionDetailDto> list = (List<InspectionDetailDto>) result.get("list");
        PageMeta meta = (PageMeta) result.get("meta");
        return Response.ok(ApiResponse.ok("Inspections retrieved successfully", list, meta)).build();
    }

    @POST
    @Path("/sync")
    @Operation(summary = "Batch offline synchronization", description = "Synchronizes multiple inspections from offline clients idempotently")
    public Response syncInspections(SyncBatchRequest request) {
        SyncBatchResponseDto responseDto = syncService.sync(request);
        return Response.ok(ApiResponse.ok("Sync batch processed successfully", responseDto)).build();
    }

    @POST
    @Path("/{id}/transition")
    @Operation(summary = "Transition inspection status", description = "Transitions status following the state machine")
    public Response transitionStatus(@PathParam("id") Long id, TransitionRequest request) {
        Inspection inspection = inspectionService.transitionStatus(id, request);
        InspectionDetailDto dto = inspectionService.toDetailDto(inspection);
        return Response.ok(ApiResponse.ok("Status transitioned successfully to " + inspection.status, dto)).build();
    }

    @GET
    @Path("/{id}/audit")
    @Operation(summary = "Get inspection audit logs", description = "Retrieves append-only audit trail from MongoDB")
    public Response getAuditLogs(@PathParam("id") Long id) {
        inspectionService.getInspectionOrThrow(id);
        List<AuditLog> logs = auditService.getAuditLogs(id);
        return Response.ok(ApiResponse.ok("Audit logs retrieved successfully", logs)).build();
    }
}

