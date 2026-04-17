package id.ac.ui.cs.advprog.mysawit.harvest.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;

public interface HarvestRepository extends JpaRepository<Harvest, UUID>, JpaSpecificationExecutor<Harvest> {
    boolean existsByBuruhIdAndHarvestDate(String buruhId, LocalDate harvestDate);
}