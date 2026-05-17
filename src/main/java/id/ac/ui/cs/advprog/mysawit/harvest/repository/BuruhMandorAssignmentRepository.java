package id.ac.ui.cs.advprog.mysawit.harvest.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;

public interface BuruhMandorAssignmentRepository
        extends JpaRepository<BuruhMandorAssignment, String> {
    Optional<BuruhMandorAssignment> findByBuruhId(String buruhId);
    boolean existsByMandorId(String mandorId);

    @Query("SELECT a.buruhId FROM BuruhMandorAssignment a WHERE a.mandorId = :mandorId")
    List<String> findBuruhIdsByMandorId(@Param("mandorId") String mandorId);
}
