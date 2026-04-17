package id.ac.ui.cs.advprog.mysawit.harvest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestCreateRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAlreadyExistsException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.BuruhMandorAssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.HarvestRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestSubmissionContext;

@ExtendWith(MockitoExtension.class)
class HarvestServiceTest {

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");

    @Mock
    private HarvestRepository harvestRepository;

    @Mock
    private BuruhMandorAssignmentRepository assignmentRepository;

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

    private BuruhMandorAssignment validAssignment() {
        BuruhMandorAssignment assignment = new BuruhMandorAssignment();
        assignment.setBuruhId("buruh-1");
        assignment.setMandorId("mandor-1");
        assignment.setBuruhName("Slamet Raharjo");
        assignment.setPlantationId("plantation-1");
        return assignment;
    }
}