package id.ac.ui.cs.advprog.mysawit.harvest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApproveHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestCreateRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.event.HarvestApprovedEvent;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAlreadyExistsException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestConflictException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.BuruhMandorAssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.HarvestRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestReviewerContext;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestSubmissionContext;

@ExtendWith(MockitoExtension.class)
class HarvestServiceTest {

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");

    @Mock
    private HarvestRepository harvestRepository;

    @Mock
    private BuruhMandorAssignmentRepository assignmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @TempDir
    Path tempDir;

    private HarvestService harvestService;

    @BeforeEach
    void setUp() {
        harvestService = new HarvestService(harvestRepository, assignmentRepository, tempDir.toString());
    }

    @Test
    void createHarvest_shouldPersistValidSubmission() {
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(java.util.Optional.of(validAssignment()));
        when(harvestRepository.existsByBuruhIdAndHarvestDate("buruh-1", LocalDate.now(JAKARTA_ZONE))).thenReturn(false);
        when(harvestRepository.save(any(Harvest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HarvestCreateRequest request = new HarvestCreateRequest();
        request.setWeightKg(new BigDecimal("350.5"));
        request.setNotes("Hasil panen pagi hari di blok A");

        MockMultipartFile photo = new MockMultipartFile(
                "photos",
                "foto1.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});

        HarvestResponse response = harvestService.createHarvest(
                request,
                new HarvestSubmissionContext("buruh-1", "Slamet Raharjo", "plantation-1", "BURUH"),
                List.of(photo));

        assertEquals("buruh-1", response.getBuruhId());
        assertEquals("Slamet Raharjo", response.getBuruhName());
        assertEquals(new BigDecimal("350.5"), response.getWeightKg());
        assertEquals("Hasil panen pagi hari di blok A", response.getNotes());
        assertEquals(LocalDate.now(JAKARTA_ZONE), response.getHarvestDate());
        assertEquals(1, response.getPhotoUrls().size());
        assertFalse(response.getPhotoUrls().get(0).isBlank());
    }

    @Test
    void createHarvest_shouldRejectDuplicateSubmissionForDay() {
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(java.util.Optional.of(validAssignment()));
        when(harvestRepository.existsByBuruhIdAndHarvestDate("buruh-1", LocalDate.now(JAKARTA_ZONE))).thenReturn(true);

        HarvestCreateRequest request = new HarvestCreateRequest();
        request.setWeightKg(new BigDecimal("100"));
        request.setNotes("Valid notes");

        MockMultipartFile photo = new MockMultipartFile("photos", "foto1.jpg", "image/jpeg", new byte[] {1});

        HarvestAlreadyExistsException exception = assertThrows(HarvestAlreadyExistsException.class, () ->
                harvestService.createHarvest(
                        request,
                        new HarvestSubmissionContext("buruh-1", "Slamet Raharjo", "plantation-1", "BURUH"),
                        List.of(photo)));

        assertEquals(HarvestErrorKey.HARVEST_ALREADY_SUBMITTED_TODAY, exception.getErrorKey());
    }

    @Test
    void createHarvest_shouldRejectMissingPhotos() {
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(java.util.Optional.of(validAssignment()));
        HarvestCreateRequest request = new HarvestCreateRequest();
        request.setWeightKg(new BigDecimal("100"));
        request.setNotes("Valid notes");

        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestService.createHarvest(
                        request,
                        new HarvestSubmissionContext("buruh-1", "Slamet Raharjo", "plantation-1", "BURUH"),
                        List.of()));

        assertEquals(HarvestErrorKey.NO_PHOTOS_PROVIDED, exception.getErrorKey());
    }

    @Test
    void createHarvest_shouldRejectInvalidPhotoType() {
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(java.util.Optional.of(validAssignment()));
        HarvestCreateRequest request = new HarvestCreateRequest();
        request.setWeightKg(new BigDecimal("100"));
        request.setNotes("Valid notes");

        MockMultipartFile photo = new MockMultipartFile("photos", "bad.txt", "text/plain", new byte[] {1});

        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestService.createHarvest(
                        request,
                        new HarvestSubmissionContext("buruh-1", "Slamet Raharjo", "plantation-1", "BURUH"),
                        List.of(photo)));

        assertEquals(HarvestErrorKey.INVALID_PHOTO_TYPE, exception.getErrorKey());
    }

    @Test
    void createHarvest_shouldRejectOversizedPhoto() {
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(java.util.Optional.of(validAssignment()));
        HarvestCreateRequest request = new HarvestCreateRequest();
        request.setWeightKg(new BigDecimal("100"));
        request.setNotes("Valid notes");

        byte[] payload = new byte[(5 * 1024 * 1024) + 1];
        MockMultipartFile photo = new MockMultipartFile("photos", "big.jpg", "image/jpeg", payload);

        HarvestValidationException exception = assertThrows(HarvestValidationException.class, () ->
                harvestService.createHarvest(
                        request,
                        new HarvestSubmissionContext("buruh-1", "Slamet Raharjo", "plantation-1", "BURUH"),
                        List.of(photo)));

        assertEquals(HarvestErrorKey.PHOTO_TOO_LARGE, exception.getErrorKey());
    }

    @Test
    void createHarvest_shouldRejectWhenBuruhNotAssignedToMandor() {
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(java.util.Optional.empty());

        HarvestCreateRequest request = new HarvestCreateRequest();
        request.setWeightKg(new BigDecimal("100"));
        request.setNotes("Valid notes");

        MockMultipartFile photo = new MockMultipartFile("photos", "foto1.jpg", "image/jpeg", new byte[] {1});

        HarvestAuthorizationException exception = assertThrows(HarvestAuthorizationException.class, () ->
                harvestService.createHarvest(
                        request,
                        new HarvestSubmissionContext("buruh-1", "Slamet Raharjo", "plantation-1", "BURUH"),
                        List.of(photo)));

        assertEquals(HarvestErrorKey.BURUH_NOT_ASSIGNED_TO_MANDOR, exception.getErrorKey());
    }

    @Test
    void approveHarvest_shouldApproveWhenMandorSupervisesBuruh() {
        UUID harvestId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Harvest harvest = pendingHarvest(harvestId);

        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.of(harvest));
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(Optional.of(validAssignment()));
        when(harvestRepository.save(any(Harvest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApproveHarvestResponse response = harvestService.approveHarvest(
                harvestId,
                new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso"));

        assertEquals(harvestId, response.getId());
        assertEquals(HarvestStatus.APPROVED, response.getStatus());
        assertEquals("Budi Santoso", response.getApprovedBy());
        assertEquals("QUEUED", response.getPayrollStatus());
        assertNotNull(response.getApprovedAt());
        assertEquals(HarvestStatus.APPROVED, harvest.getStatus());
        assertEquals(harvest.getApprovedAt(), harvest.getReviewedAt());
    }

    @Test
    void approveHarvest_shouldRejectNonSupervisingMandor() {
        UUID harvestId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Harvest harvest = pendingHarvest(harvestId);
        BuruhMandorAssignment assignment = validAssignment();
        assignment.setMandorId("mandor-2");

        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.of(harvest));
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(Optional.of(assignment));

        HarvestAuthorizationException exception = assertThrows(HarvestAuthorizationException.class, () ->
                harvestService.approveHarvest(
                        harvestId,
                        new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso")));

        assertEquals(HarvestErrorKey.MANDOR_NOT_AUTHORIZED, exception.getErrorKey());
        verify(harvestRepository, never()).save(any(Harvest.class));
    }

    @Test
    void approveHarvest_shouldRejectNonMandorRole() {
        UUID harvestId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        HarvestAuthorizationException exception = assertThrows(HarvestAuthorizationException.class, () ->
                harvestService.approveHarvest(
                        harvestId,
                        new HarvestReviewerContext("buruh-1", "BURUH", "Slamet Raharjo")));

        assertEquals(HarvestErrorKey.FORBIDDEN, exception.getErrorKey());
        verify(harvestRepository, never()).findByIdForUpdate(any(UUID.class));
    }

    @Test
    void approveHarvest_shouldReturnNotFoundWhenHarvestMissing() {
        UUID harvestId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.empty());

        HarvestNotFoundException exception = assertThrows(HarvestNotFoundException.class, () ->
                harvestService.approveHarvest(
                        harvestId,
                        new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso")));

        assertEquals(HarvestErrorKey.HARVEST_NOT_FOUND, exception.getErrorKey());
    }

    @Test
    void approveHarvest_shouldRejectAlreadyApprovedHarvest() {
        UUID harvestId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Harvest harvest = pendingHarvest(harvestId);
        harvest.setStatus(HarvestStatus.APPROVED);
        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.of(harvest));

        HarvestConflictException exception = assertThrows(HarvestConflictException.class, () ->
                harvestService.approveHarvest(
                        harvestId,
                        new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso")));

        assertEquals(HarvestErrorKey.HARVEST_ALREADY_REVIEWED, exception.getErrorKey());
    }

    @Test
    void approveHarvest_shouldRejectAlreadyRejectedHarvest() {
        UUID harvestId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Harvest harvest = pendingHarvest(harvestId);
        harvest.setStatus(HarvestStatus.REJECTED);
        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.of(harvest));

        HarvestConflictException exception = assertThrows(HarvestConflictException.class, () ->
                harvestService.approveHarvest(
                        harvestId,
                        new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso")));

        assertEquals(HarvestErrorKey.HARVEST_ALREADY_REVIEWED, exception.getErrorKey());
    }

    @Test
    void approveHarvest_shouldPublishHarvestApprovedEvent() {
        UUID harvestId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        HarvestService serviceWithEvents = new HarvestService(
                harvestRepository,
                assignmentRepository,
                eventPublisher,
                tempDir.toString());

        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.of(pendingHarvest(harvestId)));
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(Optional.of(validAssignment()));
        when(harvestRepository.save(any(Harvest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        serviceWithEvents.approveHarvest(
                harvestId,
                new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso"));

        ArgumentCaptor<HarvestApprovedEvent> eventCaptor = ArgumentCaptor.forClass(HarvestApprovedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        HarvestApprovedEvent event = eventCaptor.getValue();
        assertEquals(harvestId, event.harvestId());
        assertEquals("buruh-1", event.buruhId());
        assertEquals(new BigDecimal("120.50"), event.weightKg());
        assertNotNull(event.approvedAt());
    }

    @Test
    void approveHarvest_shouldPublishEventAfterTransactionCommit() {
        UUID harvestId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        HarvestService serviceWithEvents = new HarvestService(
                harvestRepository,
                assignmentRepository,
                eventPublisher,
                tempDir.toString());

        when(harvestRepository.findByIdForUpdate(harvestId)).thenReturn(Optional.of(pendingHarvest(harvestId)));
        when(assignmentRepository.findByBuruhId("buruh-1")).thenReturn(Optional.of(validAssignment()));
        when(harvestRepository.save(any(Harvest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            serviceWithEvents.approveHarvest(
                    harvestId,
                    new HarvestReviewerContext("mandor-1", "MANDOR", "Budi Santoso"));

            verify(eventPublisher, never()).publishEvent(any(HarvestApprovedEvent.class));

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(eventPublisher).publishEvent(any(HarvestApprovedEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private BuruhMandorAssignment validAssignment() {
        BuruhMandorAssignment assignment = new BuruhMandorAssignment();
        assignment.setBuruhId("buruh-1");
        assignment.setMandorId("mandor-1");
        assignment.setBuruhName("Slamet Raharjo");
        assignment.setPlantationId("plantation-1");
        return assignment;
    }

    private Harvest pendingHarvest(UUID harvestId) {
        Harvest harvest = new Harvest();
        harvest.setId(harvestId);
        harvest.setPlantationId("plantation-1");
        harvest.setBuruhId("buruh-1");
        harvest.setBuruhName("Slamet Raharjo");
        harvest.setWeightKg(new BigDecimal("120.50"));
        harvest.setNotes("Valid notes");
        harvest.setHarvestDate(LocalDate.now(JAKARTA_ZONE));
        harvest.setCreatedAt(java.time.LocalDateTime.now(JAKARTA_ZONE));
        harvest.setStatus(HarvestStatus.PENDING);
        return harvest;
    }
}
