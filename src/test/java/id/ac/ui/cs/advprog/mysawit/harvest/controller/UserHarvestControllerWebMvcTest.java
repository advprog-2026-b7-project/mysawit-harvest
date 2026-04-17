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
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestJwtClaimsResolver;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestViewerContext;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestHistoryService;

@WebMvcTest(UserHarvestController.class)
@Import(HarvestExceptionHandler.class)
class UserHarvestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HarvestHistoryService harvestHistoryService;

    @MockBean
    private HarvestJwtClaimsResolver claimsResolver;

    @Test
    void getHarvestHistoryByBuruhId_shouldReturnPagedResult() throws Exception {
        HarvestViewerContext viewer = new HarvestViewerContext("mandor-1", "MANDOR");
        HarvestResponse item = new HarvestResponse();
        item.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        item.setBuruhId("buruh-1");
        item.setBuruhName("Slamet Raharjo");
        item.setWeightKg(new BigDecimal("99.75"));
        item.setStatus(HarvestStatus.APPROVED);
        item.setHarvestDate(LocalDate.of(2025, 7, 20));
        item.setCreatedAt(LocalDateTime.of(2025, 7, 20, 9, 15));

        HarvestPageResponse page = new HarvestPageResponse(List.of(item), 0, 20, 1, 1);

        when(claimsResolver.resolveViewer("Bearer profile-token")).thenReturn(viewer);
        when(harvestHistoryService.getHarvestHistoryByBuruhId(
                eq(viewer),
                eq("buruh-1"),
                eq(LocalDate.of(2025, 7, 1)),
                eq(LocalDate.of(2025, 7, 31)),
                eq("APPROVED"),
                any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/users/{buruhId}/harvests", "buruh-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer profile-token")
                        .param("startDate", "2025-07-01")
                        .param("endDate", "2025-07-31")
                        .param("status", "APPROVED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].buruhName").value("Slamet Raharjo"))
                .andExpect(jsonPath("$.data.content[0].status").value("APPROVED"));
    }

    @Test
    void getHarvestHistoryByBuruhId_shouldReturnErrorContractWhenUserNotFound() throws Exception {
        HarvestViewerContext viewer = new HarvestViewerContext("admin-1", "ADMIN");

        when(claimsResolver.resolveViewer("Bearer admin-token")).thenReturn(viewer);
        when(harvestHistoryService.getHarvestHistoryByBuruhId(
                eq(viewer),
                eq("buruh-x"),
                any(),
                any(),
                any(),
                any(Pageable.class)))
                .thenThrow(new HarvestNotFoundException(HarvestErrorKey.USER_NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/v1/users/{buruhId}/harvests", "buruh-x")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.errorKey").value("USER_NOT_FOUND"));
    }
}