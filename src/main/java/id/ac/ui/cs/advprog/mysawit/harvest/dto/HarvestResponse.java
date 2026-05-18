package id.ac.ui.cs.advprog.mysawit.harvest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;

public class HarvestResponse {

    private UUID id;
    private String buruhId;
    private String buruhName;
    private BigDecimal weightKg;
    private String notes;
    private HarvestStatus status;
    private String rejectionReason;
    private LocalDate harvestDate;
    private Instant createdAt;
    private Instant reviewedAt;
    private List<String> photoUrls;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBuruhId() {
        return buruhId;
    }

    public void setBuruhId(String buruhId) {
        this.buruhId = buruhId;
    }

    public String getBuruhName() {
        return buruhName;
    }

    public void setBuruhName(String buruhName) {
        this.buruhName = buruhName;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public HarvestStatus getStatus() {
        return status;
    }

    public void setStatus(HarvestStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }
}
