package id.ac.ui.cs.advprog.mysawit.harvest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.BuruhMandorAssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.HarvestRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestViewerContext;

@ExtendWith(MockitoExtension.class)
class HarvestHistoryServiceTest {

    @Mock
    private HarvestRepository harvestRepository;

    @Mock
    private BuruhMandorAssignmentRepository assignmentRepository;

    private HarvestHistoryService harvestHistoryService;

    @BeforeEach
    void setUp() {
        harvestHistoryService = new HarvestHistoryService(harvestRepository, assignmentRepository);
    }

    @Test
    void getHarvestHistory_shouldRejectInvalidDateRange() {
        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestHistoryService.getHarvestHistory(
                        new HarvestViewerContext("buruh-1", "BURUH"),
                        LocalDate.of(2025, 7, 21),
                        LocalDate.of(2025, 7, 20),
                        null,
                        null,
                        PageRequest.of(0, 20)));

        assertEquals(HarvestErrorKey.INVALID_DATE_RANGE, exception.getErrorKey());
    }

    @Test
    void getHarvestHistory_shouldRejectInvalidStatusValue() {
        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestHistoryService.getHarvestHistory(
                        new HarvestViewerContext("buruh-1", "BURUH"),
                        null,
                        null,
                        "WAITING",
                        null,
                        PageRequest.of(0, 20)));

        assertEquals(HarvestErrorKey.INVALID_STATUS_VALUE, exception.getErrorKey());
    }

    @Test
    void getHarvestHistory_shouldRejectSizeOver100() {
        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestHistoryService.getHarvestHistory(
                        new HarvestViewerContext("admin-1", "ADMIN"),
                        null,
                        null,
                        null,
                        null,
                        PageRequest.of(0, 101)));

        assertEquals(HarvestErrorKey.VALIDATION_FAILED, exception.getErrorKey());
    }

    @Test
    void getHarvestHistory_mandorWithNoSupervisedBuruh_shouldReturnEmptyPage() {
        when(assignmentRepository.findBuruhIdsByMandorId("mandor-1")).thenReturn(List.of());

        HarvestPageResponse response = harvestHistoryService.getHarvestHistory(
                new HarvestViewerContext("mandor-1", "MANDOR"),
                null,
                null,
                null,
                "slamet",
                PageRequest.of(0, 20));

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getContent().size());
        verify(harvestRepository, never()).findAll(
                any(Specification.class),
                any(Pageable.class));
    }

    @Test
    void getHarvestHistoryByBuruhId_shouldRejectMandorNotAuthorized() {
        BuruhMandorAssignment assignment = new BuruhMandorAssignment();
        assignment.setBuruhId("buruh-1");
        assignment.setMandorId("mandor-2");
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(Optional.of(assignment));

        HarvestAuthorizationException exception = assertThrows(
                HarvestAuthorizationException.class, () ->
                harvestHistoryService.getHarvestHistoryByBuruhId(
                        new HarvestViewerContext("mandor-1", "MANDOR"),
                        "buruh-1",
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20)));

        assertEquals(HarvestErrorKey.MANDOR_NOT_AUTHORIZED, exception.getErrorKey());
    }

    @Test
    void getHarvestHistoryByBuruhId_shouldReturnUserNotFoundWhenMissing() {
        when(assignmentRepository.findByBuruhId("buruh-x")).thenReturn(Optional.empty());
        when(assignmentRepository.existsByMandorId("buruh-x")).thenReturn(false);

        HarvestNotFoundException exception = assertThrows(HarvestNotFoundException.class, () ->
                harvestHistoryService.getHarvestHistoryByBuruhId(
                        new HarvestViewerContext("admin-1", "ADMIN"),
                        "buruh-x",
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20)));

        assertEquals(HarvestErrorKey.USER_NOT_FOUND, exception.getErrorKey());
    }

    @Test
    void getHarvestHistoryByBuruhId_shouldReturnUserNotBuruhWhenTargetIsMandor() {
        when(assignmentRepository.findByBuruhId("mandor-1")).thenReturn(Optional.empty());
        when(assignmentRepository.existsByMandorId("mandor-1")).thenReturn(true);

        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestHistoryService.getHarvestHistoryByBuruhId(
                        new HarvestViewerContext("admin-1", "ADMIN"),
                        "mandor-1",
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20)));

        assertEquals(HarvestErrorKey.USER_NOT_BURUH, exception.getErrorKey());
    }

    @Test
    void getHarvestHistory_shouldReturnPagedDataForAdmin() {
        Harvest harvest = new Harvest();
        harvest.setBuruhId("buruh-1");
        harvest.setBuruhName("Slamet Raharjo");
        harvest.setWeightKg(new java.math.BigDecimal("120.5"));
        harvest.setNotes("Catatan panen");
        harvest.setHarvestDate(LocalDate.of(2025, 7, 21));
        harvest.setCreatedAt(java.time.LocalDateTime.of(2025, 7, 21, 8, 0));
        harvest.setPhotos(new java.util.ArrayList<>());

        Page<Harvest> page = new PageImpl<>(List.of(harvest), PageRequest.of(0, 20), 1);
        when(harvestRepository.findAll(
                any(Specification.class),
                any(Pageable.class)))
                .thenReturn(page);

        HarvestPageResponse response = harvestHistoryService.getHarvestHistory(
                new HarvestViewerContext("admin-1", "ADMIN"),
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20));

        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals("buruh-1", response.getContent().get(0).getBuruhId());
    }
}
