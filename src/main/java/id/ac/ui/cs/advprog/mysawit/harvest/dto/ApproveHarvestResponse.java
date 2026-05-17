package id.ac.ui.cs.advprog.mysawit.harvest.dto;

import java.time.Instant;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;

public class ApproveHarvestResponse {

    private UUID id;
    private HarvestStatus status;
    private String approvedBy;
    private Instant approvedAt;
    private String payrollStatus;

    public ApproveHarvestResponse(UUID id,
            HarvestStatus status,
            String approvedBy,
            Instant approvedAt,
            String payrollStatus) {
        this.id = id;
        this.status = status;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.payrollStatus = payrollStatus;
    }

    public UUID getId() {
        return id;
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public String getPayrollStatus() {
        return payrollStatus;
    }
}
