package id.ac.ui.cs.advprog.mysawit.harvest.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApproveHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestConflictException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestJwtClaimsResolver;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestReviewerContext;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestViewerContext;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestHistoryService;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestService;

@WebMvcTest(HarvestController.class)
@Import(HarvestExceptionHandler.class)
class HarvestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HarvestService harvestService;

    @MockBean
    private HarvestHistoryService harvestHistoryService;

    @MockBean
    private HarvestJwtClaimsResolver claimsResolver;

    @Test
    void getHarvestHistory_shouldReturnPagedResultForH04() throws Exception {
        HarvestViewerContext viewer = new HarvestViewerContext("admin-1", "ADMIN");
        HarvestResponse item = new HarvestResponse();
        item.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        item.setBuruhId("buruh-1");
        item.setBuruhName("Slamet Raharjo");
        item.setWeightKg(new BigDecimal("120.50"));
        item.setStatus(HarvestStatus.PENDING);
        item.setHarvestDate(LocalDate.of(2025, 7, 21));
        item.setCreatedAt(LocalDateTime.of(2025, 7, 21, 8, 0));
        item.setPhotoUrls(List.of("/uploads/harvests/photo-1.jpg"));

        HarvestPageResponse page = new HarvestPageResponse(List.of(item), 0, 20, 1, 1);

        when(claimsResolver.resolveViewer("Bearer test-token")).thenReturn(viewer);
        when(harvestHistoryService.getHarvestHistory(
                eq(viewer),
                eq(LocalDate.of(2025, 7, 1)),
                eq(LocalDate.of(2025, 7, 31)),
                eq("PENDING"),
                eq("Slamet"),
                any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/harvests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .param("startDate", "2025-07-01")
                        .param("endDate", "2025-07-31")
                        .param("status", "PENDING")
                        .param("buruhName", "Slamet")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].buruhId").value("buruh-1"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    void getHarvestHistory_shouldReturnErrorContractWhenForbidden() throws Exception {
        HarvestViewerContext viewer = new HarvestViewerContext("buruh-1", "BURUH");

        when(claimsResolver.resolveViewer("Bearer denied-token")).thenReturn(viewer);
        when(harvestHistoryService.getHarvestHistory(
                eq(viewer),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)))
                .thenThrow(new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/v1/harvests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer denied-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("FORBIDDEN"));
    }

    @Test
    void approveHarvest_shouldReturnApprovedResponseForH02() throws Exception {
        UUID harvestId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        HarvestReviewerContext reviewer = new HarvestReviewerContext(
                "mandor-1",
                "MANDOR",
                "Budi Santoso");
        ApproveHarvestResponse response = new ApproveHarvestResponse(
                harvestId,
                HarvestStatus.APPROVED,
                "Budi Santoso",
                LocalDateTime.of(2025, 7, 21, 10, 15),
                "QUEUED");

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.approveHarvest(harvestId, reviewer)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(harvestId.toString()))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.approvedBy").value("Budi Santoso"))
                .andExpect(jsonPath("$.data.approvedAt").value("2025-07-21T10:15:00"))
                .andExpect(jsonPath("$.data.payrollStatus").value("QUEUED"));
    }

    @Test
    void approveHarvest_shouldReturnForbiddenForNonMandor() throws Exception {
        UUID harvestId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(claimsResolver.resolveMandor("Bearer buruh-token"))
                .thenThrow(new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN,
                        "Caller does not have the MANDOR role"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer buruh-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("FORBIDDEN"));
    }

    @Test
    void approveHarvest_shouldReturnForbiddenForNonSupervisingMandor() throws Exception {
        UUID harvestId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        HarvestReviewerContext reviewer = mandorReviewer();

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.approveHarvest(harvestId, reviewer))
                .thenThrow(new HarvestAuthorizationException(
                        HarvestErrorKey.MANDOR_NOT_AUTHORIZED,
                        "Authenticated mandor does not supervise this buruh"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("MANDOR_NOT_AUTHORIZED"));
    }

    @Test
    void approveHarvest_shouldReturnNotFoundWhenHarvestIsMissing() throws Exception {
        UUID harvestId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        HarvestReviewerContext reviewer = mandorReviewer();

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.approveHarvest(harvestId, reviewer))
                .thenThrow(new HarvestNotFoundException(
                        HarvestErrorKey.HARVEST_NOT_FOUND,
                        "No harvest found with the given harvestId"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("HARVEST_NOT_FOUND"));
    }

    @Test
    void approveHarvest_shouldReturnConflictWhenAlreadyApproved() throws Exception {
        UUID harvestId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        HarvestReviewerContext reviewer = mandorReviewer();

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.approveHarvest(harvestId, reviewer))
                .thenThrow(new HarvestConflictException(
                        HarvestErrorKey.HARVEST_ALREADY_REVIEWED,
                        "Harvest has already been approved or rejected"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("HARVEST_ALREADY_REVIEWED"));
    }

    @Test
    void approveHarvest_shouldReturnConflictWhenAlreadyRejected() throws Exception {
        UUID harvestId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        HarvestReviewerContext reviewer = mandorReviewer();

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.approveHarvest(harvestId, reviewer))
                .thenThrow(new HarvestConflictException(
                        HarvestErrorKey.HARVEST_ALREADY_REVIEWED,
                        "Harvest has already been approved or rejected"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("HARVEST_ALREADY_REVIEWED"));
    }

    private HarvestReviewerContext mandorReviewer() {
        return new HarvestReviewerContext(
                "mandor-1",
                "MANDOR",
                "Budi Santoso");
    }
}
