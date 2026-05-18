package id.ac.ui.cs.advprog.mysawit.harvest.dto;

import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import java.time.Instant;
import java.util.UUID;

public class RejectHarvestResponse {

    private UUID id;
    private HarvestStatus status;
    private String rejectionReason;
    private String rejectedBy;
    private Instant rejectedAt;

    public RejectHarvestResponse(UUID id,
            HarvestStatus status,
            String rejectionReason,
            String rejectedBy,
            Instant rejectedAt) {
        this.id = id;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.rejectedBy = rejectedBy;
        this.rejectedAt = rejectedAt;
    }

    public UUID getId() {
        return id;
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }
}
