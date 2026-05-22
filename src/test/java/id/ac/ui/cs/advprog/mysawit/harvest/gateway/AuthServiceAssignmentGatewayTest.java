package id.ac.ui.cs.advprog.mysawit.harvest.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withForbiddenRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class AuthServiceAssignmentGatewayTest {

    private static final String AUTH_BASE_URL = "http://auth.test";
    private static final String BEARER_TOKEN = "Bearer test-token";

    private MockRestServiceServer server;
    private AuthServiceAssignmentGateway gateway;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        gateway = new AuthServiceAssignmentGateway(restTemplate, AUTH_BASE_URL);
    }

    @Test
    void findByBuruhIdShouldMapAssignmentResponse() {
        server.expect(requestTo(AUTH_BASE_URL + "/api/assignments/buruh/buruh-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
                .andRespond(withSuccess(singleAssignmentJson(), MediaType.APPLICATION_JSON));

        Optional<BuruhMandorAssignment> result = gateway.findByBuruhId(
                "buruh-1",
                BEARER_TOKEN);

        assertTrue(result.isPresent());
        BuruhMandorAssignment assignment = result.get();
        assertEquals("buruh-1", assignment.getBuruhId());
        assertEquals("mandor-1", assignment.getMandorId());
        assertEquals("Slamet Raharjo", assignment.getBuruhName());
        assertEquals("plantation-1", assignment.getPlantationId());
        assertEquals(LocalDateTime.parse("2026-05-01T10:15:30"), assignment.getAssignedAt());
        server.verify();
    }

    @Test
    void findByBuruhIdShouldReturnEmptyWhenAuthServiceReturnsNotFound() {
        server.expect(requestTo(AUTH_BASE_URL + "/api/assignments/buruh/missing-buruh"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withResourceNotFound());

        Optional<BuruhMandorAssignment> result = gateway.findByBuruhId(
                "missing-buruh",
                null);

        assertFalse(result.isPresent());
        server.verify();
    }

    @Test
    void findByBuruhIdShouldReturnEmptyWhenAuthServiceReturnsForbidden() {
        server.expect(requestTo(AUTH_BASE_URL + "/api/assignments/buruh/forbidden-buruh"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withForbiddenRequest());

        Optional<BuruhMandorAssignment> result = gateway.findByBuruhId(
                "forbidden-buruh",
                "");

        assertFalse(result.isPresent());
        server.verify();
    }

    @Test
    void findBuruhIdsByMandorIdShouldFilterBlankIds() {
        server.expect(requestTo(AUTH_BASE_URL + "/api/assignments/mandor/mandor-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN))
                .andRespond(withSuccess(mandorAssignmentsJson(), MediaType.APPLICATION_JSON));

        List<String> result = gateway.findBuruhIdsByMandorId("mandor-1", BEARER_TOKEN);

        assertEquals(List.of("buruh-1", "buruh-2"), result);
        server.verify();
    }

    @Test
    void findBuruhIdsByMandorIdShouldReturnEmptyWhenResponseHasNoContent() {
        server.expect(requestTo(AUTH_BASE_URL + "/api/assignments/mandor/mandor-empty"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"success\",\"data\":{}}",
                        MediaType.APPLICATION_JSON));

        List<String> result = gateway.findBuruhIdsByMandorId("mandor-empty", null);

        assertTrue(result.isEmpty());
        server.verify();
    }

    @Test
    void findBuruhIdsByMandorIdShouldReturnEmptyWhenAuthServiceRejectsRequest() {
        server.expect(requestTo(AUTH_BASE_URL + "/api/assignments/mandor/mandor-forbidden"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withForbiddenRequest());

        List<String> result = gateway.findBuruhIdsByMandorId("mandor-forbidden", null);

        assertTrue(result.isEmpty());
        server.verify();
    }

    private String singleAssignmentJson() {
        return """
                {
                  "status": "success",
                  "data": {
                    "buruhId": "buruh-1",
                    "buruhNama": "Slamet Raharjo",
                    "mandorId": "mandor-1",
                    "mandorNama": "Budi Santoso",
                    "plantationId": "plantation-1",
                    "assignedAt": "2026-05-01T10:15:30"
                  }
                }
                """;
    }

    private String mandorAssignmentsJson() {
        return """
                {
                  "status": "success",
                  "data": {
                    "content": [
                      {"buruhId": "buruh-1"},
                      {"buruhId": ""},
                      {"buruhId": null},
                      {"buruhId": "buruh-2"}
                    ]
                  }
                }
                """;
    }
}
