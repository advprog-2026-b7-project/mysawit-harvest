package id.ac.ui.cs.advprog.mysawit.harvest.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApproveHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestCreateRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.RejectHarvestRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.RejectHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.event.HarvestApprovedEvent;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAlreadyExistsException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestConflictException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestStorageException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestPhoto;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.BuruhMandorAssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.HarvestRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestReviewerContext;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestSubmissionContext;

@Service
public class HarvestService {

    private static final ZoneId JAKARTA_ZONE = ZoneId.of("Asia/Jakarta");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PHOTO_COUNT = 10;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp");

    private final HarvestRepository harvestRepository;
    private final BuruhMandorAssignmentRepository assignmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Path storageRoot;
    private final String publicBaseUrl;

    @Autowired
    public HarvestService(HarvestRepository harvestRepository,
            BuruhMandorAssignmentRepository assignmentRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${storage.harvest.dir:uploads/harvests}") String storageDir,
            @Value("${storage.harvest.public-base-url:https://storage.mysawit.id}")
            String publicBaseUrl) {
        this.harvestRepository = harvestRepository;
        this.assignmentRepository = assignmentRepository;
        this.eventPublisher = eventPublisher;
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
        this.publicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
    }

    HarvestService(HarvestRepository harvestRepository,
            BuruhMandorAssignmentRepository assignmentRepository,
            String storageDir) {
        this(harvestRepository, assignmentRepository, null, storageDir);
    }

    HarvestService(HarvestRepository harvestRepository,
            BuruhMandorAssignmentRepository assignmentRepository,
            ApplicationEventPublisher eventPublisher,
            String storageDir) {
        this(
                harvestRepository,
                assignmentRepository,
                eventPublisher,
                storageDir,
                "https://storage.mysawit.id");
    }

    @Transactional
    public HarvestResponse createHarvest(HarvestCreateRequest request,
            HarvestSubmissionContext context,
            List<MultipartFile> photos) {
        validateRequest(request);
        BuruhMandorAssignment assignment = validateAndGetAssignment(context);
        validateContextCompatibility(context, assignment);
        validatePhotos(photos);
        ensureUniqueForToday(context.buruhId());

        Harvest harvest = new Harvest();
        harvest.setPlantationId(resolvePlantationId(context, assignment));
        harvest.setBuruhId(context.buruhId());
        harvest.setBuruhName(resolveBuruhName(context, assignment));
        harvest.setWeightKg(request.getWeightKg());
        harvest.setNotes(request.getNotes());
        harvest.setHarvestDate(LocalDate.now(JAKARTA_ZONE));
        harvest.setStatus(HarvestStatus.PENDING);

        if (photos != null) {
            photos.stream()
                    .filter(photo -> photo != null && !photo.isEmpty())
                    .forEach(photo -> harvest.addPhoto(buildPhotoEntity(photo)));
        }

        try {
            Harvest saved = harvestRepository.save(harvest);
            return HarvestResponseMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new HarvestAlreadyExistsException("Harvest for today already submitted");
        }
    }

    @Transactional
    public ApproveHarvestResponse approveHarvest(UUID harvestId, HarvestReviewerContext reviewer) {
        Harvest harvest = findPendingHarvestForReview(harvestId, reviewer);

        Instant approvedAt = Instant.now();
        LocalDateTime approvedAtInJakarta = LocalDateTime.ofInstant(approvedAt, JAKARTA_ZONE);
        String approvedBy = resolveReviewerName(reviewer);

        harvest.setStatus(HarvestStatus.APPROVED);
        harvest.setApprovedBy(approvedBy);
        harvest.setApprovedAt(approvedAtInJakarta);
        harvest.setReviewedAt(approvedAtInJakarta);

        Harvest saved = harvestRepository.save(harvest);
        publishHarvestApprovedEvent(new HarvestApprovedEvent(
                saved.getId(),
                saved.getBuruhId(),
                saved.getWeightKg(),
                approvedAt));

        return new ApproveHarvestResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getApprovedBy(),
                approvedAt,
                "QUEUED");
    }

    @Transactional
    public RejectHarvestResponse rejectHarvest(UUID harvestId,
            HarvestReviewerContext reviewer,
            RejectHarvestRequest request) {
        String rejectionReason = validateRejectionReason(request);
        Harvest harvest = findPendingHarvestForReview(harvestId, reviewer);

        Instant rejectedAt = Instant.now();
        LocalDateTime rejectedAtInJakarta = LocalDateTime.ofInstant(rejectedAt, JAKARTA_ZONE);
        String rejectedBy = resolveReviewerName(reviewer);

        harvest.setStatus(HarvestStatus.REJECTED);
        harvest.setRejectionReason(rejectionReason);
        harvest.setRejectedBy(rejectedBy);
        harvest.setRejectedAt(rejectedAtInJakarta);
        harvest.setReviewedAt(rejectedAtInJakarta);

        Harvest saved = harvestRepository.save(harvest);
        return new RejectHarvestResponse(
                saved.getId(),
                saved.getStatus(),
                saved.getRejectionReason(),
                saved.getRejectedBy(),
                rejectedAt);
    }

    private Harvest findPendingHarvestForReview(UUID harvestId, HarvestReviewerContext reviewer) {
        validateReviewerContext(reviewer);

        Harvest harvest = harvestRepository.findByIdForUpdate(harvestId)
                .orElseThrow(() -> new HarvestNotFoundException(HarvestErrorKey.HARVEST_NOT_FOUND,
                        "No harvest found with the given harvestId"));

        if (harvest.getStatus() != HarvestStatus.PENDING) {
            throw new HarvestConflictException(HarvestErrorKey.HARVEST_ALREADY_REVIEWED,
                    "Harvest has already been approved or rejected");
        }

        validateReviewerSupervisesBuruh(reviewer, harvest);
        return harvest;
    }

    private void validateReviewerSupervisesBuruh(HarvestReviewerContext reviewer,
            Harvest harvest) {
        BuruhMandorAssignment assignment = assignmentRepository.findByBuruhId(
                        harvest.getBuruhId())
                .orElseThrow(() -> new HarvestAuthorizationException(
                        HarvestErrorKey.MANDOR_NOT_AUTHORIZED,
                        "Authenticated mandor does not supervise this buruh"));

        if (!reviewer.userId().equals(assignment.getMandorId())) {
            throw new HarvestAuthorizationException(HarvestErrorKey.MANDOR_NOT_AUTHORIZED,
                    "Authenticated mandor does not supervise this buruh");
        }
    }

    private String validateRejectionReason(RejectHarvestRequest request) {
        if (request == null
                || request.getRejectionReason() == null
                || request.getRejectionReason().isBlank()) {
            throw new HarvestValidationException(
                    HarvestErrorKey.REJECTION_REASON_REQUIRED,
                    "rejectionReason is required");
        }

        return request.getRejectionReason().trim();
    }

    private void validateReviewerContext(HarvestReviewerContext reviewer) {
        if (reviewer == null || reviewer.userId() == null || reviewer.userId().isBlank()) {
            throw new HarvestAuthorizationException("Invalid authenticated mandor context");
        }

        if (reviewer.role() == null || !reviewer.role().equalsIgnoreCase("MANDOR")) {
            throw new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN,
                    "Caller does not have the MANDOR role");
        }
    }

    private String resolveReviewerName(HarvestReviewerContext reviewer) {
        if (reviewer.name() != null && !reviewer.name().isBlank()) {
            return reviewer.name();
        }

        return reviewer.userId();
    }

    private void publishHarvestApprovedEvent(HarvestApprovedEvent event) {
        if (eventPublisher == null) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publishEvent(event);
                }
            });
            return;
        }

        eventPublisher.publishEvent(event);
    }

    private void validateRequest(HarvestCreateRequest request) {
        if (request == null
                || request.getWeightKg() == null
                || request.getWeightKg().signum() <= 0) {
            throw new HarvestValidationException("weightKg must be greater than 0");
        }

        if (request.getNotes() == null || request.getNotes().isBlank()) {
            throw new HarvestValidationException("notes is required");
        }
    }

    private BuruhMandorAssignment validateAndGetAssignment(HarvestSubmissionContext context) {
        if (context == null) {
            throw new HarvestAuthorizationException("Submission context is required");
        }

        if (context.role() == null || !context.role().equalsIgnoreCase("BURUH")) {
            throw new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN,
                    "Caller does not have the BURUH role");
        }

        return assignmentRepository.findByBuruhId(context.buruhId())
                .filter(assignment -> assignment.getMandorId() != null
                        && !assignment.getMandorId().isBlank())
                .orElseThrow(() -> new HarvestAuthorizationException(
                        HarvestErrorKey.BURUH_NOT_ASSIGNED_TO_MANDOR,
                        "Buruh is not assigned to a mandor"));
    }

    private String resolveBuruhName(HarvestSubmissionContext context,
            BuruhMandorAssignment assignment) {
        if (assignment.getBuruhName() != null && !assignment.getBuruhName().isBlank()) {
            return assignment.getBuruhName();
        }

        if (context.buruhName() != null && !context.buruhName().isBlank()) {
            return context.buruhName();
        }

        if (context.buruhId() != null && !context.buruhId().isBlank()) {
            return context.buruhId();
        }

        return "Unknown Buruh";
    }

    private String resolvePlantationId(HarvestSubmissionContext context,
            BuruhMandorAssignment assignment) {
        if (assignment.getPlantationId() != null && !assignment.getPlantationId().isBlank()) {
            return assignment.getPlantationId();
        }

        if (context.plantationId() != null && !context.plantationId().isBlank()) {
            return context.plantationId();
        }

        return "UNSPECIFIED";
    }

    private void validateContextCompatibility(HarvestSubmissionContext context,
            BuruhMandorAssignment assignment) {
        if (context.plantationId() != null
                && !context.plantationId().isBlank()
                && assignment.getPlantationId() != null
                && !assignment.getPlantationId().isBlank()
                && !context.plantationId().equals(assignment.getPlantationId())) {
            throw new HarvestAuthorizationException(HarvestErrorKey.BURUH_NOT_ASSIGNED_TO_MANDOR,
                    "Buruh is not assigned to a mandor");
        }
    }

    private void validatePhotos(List<MultipartFile> photos) {
        if (photos == null
                || photos.stream().allMatch(photo -> photo == null || photo.isEmpty())) {
            throw new HarvestValidationException(HarvestErrorKey.NO_PHOTOS_PROVIDED,
                    "At least one photo is required");
        }

        long nonEmptyCount = photos.stream()
                .filter(photo -> photo != null && !photo.isEmpty())
                .count();

        if (nonEmptyCount > MAX_PHOTO_COUNT) {
            throw new HarvestValidationException("A maximum of 10 photos is allowed");
        }

        photos.stream()
                .filter(photo -> photo != null && !photo.isEmpty())
                .forEach(this::validatePhoto);
    }

    private void validatePhoto(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new HarvestValidationException(HarvestErrorKey.PHOTO_TOO_LARGE,
                    "Photo exceeds 5 MB: " + safeFilename(file));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new HarvestValidationException(HarvestErrorKey.INVALID_PHOTO_TYPE,
                    "Unsupported photo type: " + safeFilename(file));
        }
    }

    private void ensureUniqueForToday(String buruhId) {
        LocalDate today = LocalDate.now(JAKARTA_ZONE);
        boolean exists = harvestRepository.existsByBuruhIdAndHarvestDate(buruhId, today);
        if (exists) {
            throw new HarvestAlreadyExistsException("Harvest for today already submitted");
        }
    }

    private HarvestPhoto buildPhotoEntity(MultipartFile file) {
        try {
            Files.createDirectories(storageRoot);

            String originalName = safeFilename(file);
            String extension = extractExtension(originalName);
            String filename = UUID.randomUUID() + extension;

            Path target = storageRoot.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            HarvestPhoto photo = new HarvestPhoto();
            photo.setPhotoUrl(buildPublicPhotoUrl(filename));
            photo.setOriginalFilename(originalName);
            photo.setContentType(file.getContentType());
            photo.setFileSizeBytes(file.getSize());
            return photo;
        } catch (IOException ex) {
            throw new HarvestStorageException("Failed to store harvest photo", ex);
        }
    }

    private String extractExtension(String originalName) {
        if (originalName == null || originalName.isBlank() || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.'));
    }

    private String safeFilename(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return "unknown";
        }
        return Paths.get(originalName).getFileName().toString();
    }

    private String buildPublicPhotoUrl(String filename) {
        return publicBaseUrl + "/harvests/" + filename;
    }

    private String normalizePublicBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            return "https://storage.mysawit.id";
        }

        return configuredBaseUrl.replaceAll("/+$", "");
    }
}
