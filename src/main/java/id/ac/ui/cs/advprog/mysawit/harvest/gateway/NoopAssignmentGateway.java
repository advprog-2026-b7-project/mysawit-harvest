package id.ac.ui.cs.advprog.mysawit.harvest.gateway;

import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoopAssignmentGateway implements AssignmentGateway {

    @Override
    public Optional<BuruhMandorAssignment> findByBuruhId(
            String buruhId,
            String authorizationHeader) {
        return Optional.empty();
    }

    @Override
    public List<String> findBuruhIdsByMandorId(
            String mandorId,
            String authorizationHeader) {
        return List.of();
    }
}
