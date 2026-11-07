package org.acme.vehicle.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.vehicle.dto.*;
import org.acme.vehicle.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SyncService {

    @Inject
    SingleSyncItemProcessor singleProcessor;

    public SyncBatchResponseDto sync(SyncBatchRequest request) {
        if (request == null || request.inspections == null) {
            throw new ValidationException("Request body must contain 'inspections' array",
                    List.of(new FieldErrorDetail("inspections", "Array 'inspections' is required")));
        }

        List<SyncItemResultDto> results = new ArrayList<>();
        String deviceId = request.deviceId != null ? request.deviceId : "unknown";

        for (SyncItemRequest item : request.inspections) {
            if (item == null) continue;
            try {
                SyncItemResultDto res = singleProcessor.process(item, deviceId);
                results.add(res);
            } catch (Exception e) {
                results.add(SyncItemResultDto.rejected(item.clientId,
                        List.of(new FieldErrorDetail("general", "Sync item processing error: " + e.getMessage()))));
            }
        }

        return new SyncBatchResponseDto(results);
    }
}

