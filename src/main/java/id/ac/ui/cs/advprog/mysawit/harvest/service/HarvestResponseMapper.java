package id.ac.ui.cs.advprog.mysawit.harvest.service;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

final class HarvestResponseMapper {

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");

    private HarvestResponseMapper() {
    }

    static HarvestResponse toResponse(Harvest harvest) {
        HarvestResponse response = new HarvestResponse();
        response.setId(harvest.getId());
        response.setBuruhId(harvest.getBuruhId());
        response.setBuruhName(harvest.getBuruhName());
        response.setWeightKg(harvest.getWeightKg());
        response.setNotes(harvest.getNotes());
        response.setStatus(harvest.getStatus());
        response.setRejectionReason(harvest.getRejectionReason());
        response.setHarvestDate(harvest.getHarvestDate());
        response.setCreatedAt(toInstant(harvest.getCreatedAt()));
        response.setReviewedAt(toInstant(harvest.getReviewedAt()));
        response.setPhotoUrls(harvest.getPhotos().stream()
                .map(photo -> photo.getPhotoUrl())
                .toList());
        return response;
    }

    static Instant toInstant(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.atZone(JAKARTA_ZONE).toInstant();
    }
}
