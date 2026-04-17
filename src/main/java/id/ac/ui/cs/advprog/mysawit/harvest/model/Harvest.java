package id.ac.ui.cs.advprog.mysawit.harvest.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "harvests", uniqueConstraints = {
    @UniqueConstraint(name = "uk_harvest_buruh_date", columnNames = {"buruh_id", "harvest_date"})
})
public class Harvest {

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plantation_id", nullable = false)
    private String plantationId;

    @Column(name = "buruh_id", nullable = false)
    private String buruhId;

    @Column(name = "buruh_name")
    private String buruhName;

    @Column(name = "weight_kg", nullable = false, precision = 15, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HarvestStatus status = HarvestStatus.PENDING;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "harvest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HarvestPhoto> photos = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now(JAKARTA_ZONE);
        }

        if (this.harvestDate == null) {
            this.harvestDate = LocalDate.now(JAKARTA_ZONE);
        }

        if (this.status == null) {
            this.status = HarvestStatus.PENDING;
        }
    }

    public void addPhoto(HarvestPhoto photo) {
        photo.setHarvest(this);
        this.photos.add(photo);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPlantationId() {
        return plantationId;
    }

    public void setPlantationId(String plantationId) {
        this.plantationId = plantationId;
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

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDate getHarvestDate() {
        return harvestDate;
    }

    public void setHarvestDate(LocalDate harvestDate) {
        this.harvestDate = harvestDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<HarvestPhoto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<HarvestPhoto> photos) {
        this.photos = photos;
    }
}