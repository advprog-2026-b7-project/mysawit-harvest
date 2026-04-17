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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestJwtClaimsResolver;
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
}