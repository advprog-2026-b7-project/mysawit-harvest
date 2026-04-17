package id.ac.ui.cs.advprog.mysawit.harvest.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "buruh_mandor_assignments")
public class BuruhMandorAssignment {

    @Id
    @Column(name = "buruh_id", nullable = false)
    private String buruhId;

    @Column(name = "mandor_id", nullable = false)
    private String mandorId;

    @Column(name = "buruh_name")
    private String buruhName;

    @Column(name = "plantation_id")
    private String plantationId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = LocalDateTime.now();
        }
    }

    public String getBuruhId() {
        return buruhId;
    }

    public void setBuruhId(String buruhId) {
        this.buruhId = buruhId;
    }

    public String getMandorId() {
        return mandorId;
    }

    public void setMandorId(String mandorId) {
        this.mandorId = mandorId;
    }

    public String getBuruhName() {
        return buruhName;
    }

    public void setBuruhName(String buruhName) {
        this.buruhName = buruhName;
    }

    public String getPlantationId() {
        return plantationId;
    }

    public void setPlantationId(String plantationId) {
        this.plantationId = plantationId;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
}