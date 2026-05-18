package id.ac.ui.cs.advprog.mysawit.harvest.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApproveHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.RejectHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthenticationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestConflictException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestJwtClaimsResolver;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestReviewerContext;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestSubmissionContext;
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
    void createHarvest_shouldReturnSuccessEnvelopeForH01() throws Exception {
        HarvestSubmissionContext context = new HarvestSubmissionContext(
                "buruh-1",
                "Slamet Raharjo",
                "plantation-1",
                "BURUH");
        HarvestResponse response = new HarvestResponse();
        response.setId(UUID.fromString("77777777-7777-7777-7777-777777777777"));
        response.setBuruhId("buruh-1");
        response.setBuruhName("Slamet Raharjo");
        response.setWeightKg(new BigDecimal("350.5"));
        response.setNotes("Hasil panen pagi hari di blok A");
        response.setPhotoUrls(List.of("https://storage.mysawit.id/harvests/photo-1.jpg"));
        response.setStatus(HarvestStatus.PENDING);
        response.setHarvestDate(LocalDate.of(2025, 7, 21));
        response.setCreatedAt(Instant.parse("2025-07-21T00:45:00Z"));

        MockMultipartFile photo = new MockMultipartFile(
                "photos",
                "foto1.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});

        when(claimsResolver.resolve("Bearer buruh-token")).thenReturn(context);
        when(harvestService.createHarvest(any(), eq(context), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/harvests")
                        .file(photo)
                        .param("weightKg", "350.5")
                        .param("notes", "Hasil panen pagi hari di blok A")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer buruh-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(response.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").value("2025-07-21T00:45:00Z"));
    }

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
        item.setCreatedAt(Instant.parse("2025-07-21T01:00:00Z"));
        item.setPhotoUrls(List.of("https://storage.mysawit.id/harvests/photo-1.jpg"));

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
                .thenThrow(new HarvestAuthorizationException(
                        HarvestErrorKey.FORBIDDEN,
                        "Access denied"));

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
                Instant.parse("2025-07-21T10:15:00Z"),
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
                .andExpect(jsonPath("$.data.approvedAt").value("2025-07-21T10:15:00Z"))
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
    void approveHarvest_shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        UUID harvestId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.errors[0]").value("UNAUTHORIZED"));
    }

    @Test
    void approveHarvest_shouldReturnUnauthorizedWhenJwtInvalid() throws Exception {
        UUID harvestId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        when(claimsResolver.resolveMandor("Bearer invalid-token"))
                .thenThrow(new HarvestAuthenticationException("Invalid JWT format"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/approve", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.errors[0]").value("UNAUTHORIZED"));
    }

    @Test
    void rejectHarvest_shouldReturnRejectedResponseForH03() throws Exception {
        UUID harvestId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        HarvestReviewerContext reviewer = new HarvestReviewerContext(
                "mandor-1",
                "MANDOR",
                "Budi Santoso");
        RejectHarvestResponse response = new RejectHarvestResponse(
                harvestId,
                HarvestStatus.REJECTED,
                "Berat timbangan tidak sesuai",
                "Budi Santoso",
                Instant.parse("2025-07-21T10:30:00Z"));

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.rejectHarvest(eq(harvestId), eq(reviewer), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/reject", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Berat timbangan tidak sesuai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(harvestId.toString()))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectedBy").value("Budi Santoso"))
                .andExpect(jsonPath("$.data.rejectedAt").value("2025-07-21T10:30:00Z"));
    }

    @Test
    void rejectHarvest_shouldReturnBadRequestWhenReasonMissing() throws Exception {
        UUID harvestId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        HarvestReviewerContext reviewer = new HarvestReviewerContext(
                "mandor-1",
                "MANDOR",
                "Budi Santoso");

        when(claimsResolver.resolveMandor("Bearer mandor-token")).thenReturn(reviewer);
        when(harvestService.rejectHarvest(eq(harvestId), eq(reviewer), any()))
                .thenThrow(new HarvestValidationException(
                        HarvestErrorKey.REJECTION_REASON_REQUIRED,
                        "rejectionReason is required"));

        mockMvc.perform(patch("/api/v1/harvests/{harvestId}/reject", harvestId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer mandor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("REJECTION_REASON_REQUIRED"));
    }
}
