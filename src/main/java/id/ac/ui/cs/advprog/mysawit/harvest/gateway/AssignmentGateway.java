package id.ac.ui.cs.advprog.mysawit.harvest.gateway;

import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import java.util.List;
import java.util.Optional;

public interface AssignmentGateway {

    Optional<BuruhMandorAssignment> findByBuruhId(
            String buruhId,
            String authorizationHeader);

    List<String> findBuruhIdsByMandorId(
            String mandorId,
            String authorizationHeader);
}
