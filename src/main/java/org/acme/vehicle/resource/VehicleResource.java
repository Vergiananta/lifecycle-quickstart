package org.acme.vehicle.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.vehicle.dto.ApiResponse;
import org.acme.vehicle.dto.VehicleDto;
import org.acme.vehicle.service.InspectionService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/vehicles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Vehicles", description = "Operations related to vehicles")
public class VehicleResource {

    @Inject
    InspectionService inspectionService;

    @GET
    @Operation(summary = "List vehicles", description = "Returns 20 vehicles seeded via migration")
    public Response getVehicles() {
        List<VehicleDto> vehicles = inspectionService.getAllVehicles();
        return Response.ok(ApiResponse.ok("Vehicles retrieved successfully", vehicles)).build();
    }
}

