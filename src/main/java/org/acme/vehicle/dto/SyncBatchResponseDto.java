package org.acme.vehicle.dto;

import java.util.ArrayList;
import java.util.List;

public class SyncBatchResponseDto {
    public List<SyncItemResultDto> results = new ArrayList<>();

    public SyncBatchResponseDto() {}

    public SyncBatchResponseDto(List<SyncItemResultDto> results) {
        this.results = results != null ? results : new ArrayList<>();
    }
}

