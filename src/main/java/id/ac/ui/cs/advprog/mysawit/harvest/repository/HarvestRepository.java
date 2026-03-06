package id.ac.ui.cs.advprog.mysawit.harvest.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;

public interface HarvestRepository extends JpaRepository<Harvest, UUID> {
    boolean existsByBuruhIdAndCreatedAtBetween(String buruhId, LocalDateTime start, LocalDateTime end);
}