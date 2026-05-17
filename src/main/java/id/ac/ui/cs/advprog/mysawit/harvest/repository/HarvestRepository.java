package id.ac.ui.cs.advprog.mysawit.harvest.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import jakarta.persistence.LockModeType;

public interface HarvestRepository
        extends JpaRepository<Harvest, UUID>, JpaSpecificationExecutor<Harvest> {
    boolean existsByBuruhIdAndHarvestDate(String buruhId, LocalDate harvestDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Harvest h WHERE h.id = :id")
    Optional<Harvest> findByIdForUpdate(@Param("id") UUID id);
}
