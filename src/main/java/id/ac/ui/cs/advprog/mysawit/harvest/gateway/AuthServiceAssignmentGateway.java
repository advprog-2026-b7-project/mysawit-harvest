package id.ac.ui.cs.advprog.mysawit.harvest.gateway;

import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("!test")
public class AuthServiceAssignmentGateway implements AssignmentGateway {

    private final RestTemplate restTemplate;
    private final String authServiceBaseUrl;

    public AuthServiceAssignmentGateway(
            RestTemplate restTemplate,
            @Value("${auth.service.base-url:http://localhost:8080}") String authServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.authServiceBaseUrl = authServiceBaseUrl;
    }

    @Override
    public Optional<BuruhMandorAssignment> findByBuruhId(
            String buruhId,
            String authorizationHeader) {
        String url = authServiceBaseUrl + "/api/assignments/buruh/" + buruhId;
        try {
            ResponseEntity<AssignmentApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestWithAuth(authorizationHeader),
                    AssignmentApiResponse.class);
            AssignmentResponse body = response.getBody() == null
                    ? null
                    : response.getBody().data();
            return Optional.ofNullable(toAssignment(body));
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (HttpClientErrorException.Forbidden ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<String> findBuruhIdsByMandorId(
            String mandorId,
            String authorizationHeader) {
        String url = authServiceBaseUrl + "/api/assignments/mandor/" + mandorId;
        try {
            ResponseEntity<MandorAssignmentsApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestWithAuth(authorizationHeader),
                    MandorAssignmentsApiResponse.class);
            MandorAssignmentsResponse body = response.getBody() == null
                    ? null
                    : response.getBody().data();
            if (body == null || body.content() == null) {
                return List.of();
            }
            return body.content().stream()
                    .map(AssignmentResponse::buruhId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
        } catch (HttpClientErrorException.NotFound ex) {
            return List.of();
        } catch (HttpClientErrorException.Forbidden ex) {
            return List.of();
        }
    }

    private HttpEntity<Void> requestWithAuth(String authorizationHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return new HttpEntity<>(headers);
    }

    private BuruhMandorAssignment toAssignment(AssignmentResponse response) {
        if (response == null || response.buruhId() == null || response.mandorId() == null) {
            return null;
        }

        BuruhMandorAssignment assignment = new BuruhMandorAssignment();
        assignment.setBuruhId(response.buruhId());
        assignment.setMandorId(response.mandorId());
        assignment.setBuruhName(response.buruhNama());
        assignment.setPlantationId(response.plantationId());
        assignment.setAssignedAt(response.assignedAt() != null
                ? response.assignedAt()
                : LocalDateTime.now());
        return assignment;
    }

    private record AssignmentApiResponse(
            String status,
            AssignmentResponse data) {
    }

    private record MandorAssignmentsApiResponse(
            String status,
            MandorAssignmentsResponse data) {
    }

    private record MandorAssignmentsResponse(
            List<AssignmentResponse> content) {
    }

    private record AssignmentResponse(
            String buruhId,
            String buruhNama,
            String mandorId,
            String mandorNama,
            String plantationId,
            LocalDateTime assignedAt) {
    }
}
