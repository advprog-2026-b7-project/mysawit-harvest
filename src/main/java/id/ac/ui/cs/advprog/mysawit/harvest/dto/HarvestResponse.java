package id.ac.ui.cs.advprog.mysawit.harvest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HarvestResponse {

    private UUID id;
    private String plantationId;
    private String buruhId;
    private BigDecimal weightKg;
    private String description;
    private HarvestStatus status;
    private String rejectionReason;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private List<String> photoUrls;
}