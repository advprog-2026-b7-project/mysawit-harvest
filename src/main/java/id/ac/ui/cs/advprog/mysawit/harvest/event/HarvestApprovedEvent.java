package id.ac.ui.cs.advprog.mysawit.harvest.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record HarvestApprovedEvent(
        UUID harvestId,
        String buruhId,
        BigDecimal weightKg,
        Instant approvedAt) {
}
