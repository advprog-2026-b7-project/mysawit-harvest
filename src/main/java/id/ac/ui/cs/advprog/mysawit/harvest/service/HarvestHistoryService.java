package id.ac.ui.cs.advprog.mysawit.harvest.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.error.HarvestErrorKey;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAuthorizationException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestNotFoundException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestValidationException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.BuruhMandorAssignment;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.BuruhMandorAssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.HarvestRepository;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestViewerContext;

@Service
public class HarvestHistoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final HarvestRepository harvestRepository;
    private final BuruhMandorAssignmentRepository assignmentRepository;

    public HarvestHistoryService(HarvestRepository harvestRepository,
            BuruhMandorAssignmentRepository assignmentRepository) {
        this.harvestRepository = harvestRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public HarvestPageResponse getHarvestHistory(HarvestViewerContext viewer,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            String buruhName,
            Pageable pageable) {
        validateViewerRole(viewer, "BURUH", "MANDOR", "ADMIN");
        validateDateRange(startDate, endDate);
        HarvestStatus parsedStatus = parseStatus(status);
        validatePageSize(pageable);

        String effectiveBuruhName = viewer.role().equals("BURUH") ? null : normalizeString(buruhName);

        Specification<Harvest> spec = buildCommonFilters(startDate, endDate, parsedStatus, effectiveBuruhName);

        if (viewer.role().equals("BURUH")) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("buruhId"), viewer.userId()));
        } else if (viewer.role().equals("MANDOR")) {
            List<String> supervisedBuruhIds = assignmentRepository.findBuruhIdsByMandorId(viewer.userId());
            if (supervisedBuruhIds.isEmpty()) {
                return new HarvestPageResponse(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
            }
            spec = spec.and((root, query, cb) -> root.get("buruhId").in(supervisedBuruhIds));
        }

        return toPageResponse(harvestRepository.findAll(spec, pageable));
    }

    @Transactional(readOnly = true)
    public HarvestPageResponse getHarvestHistoryByBuruhId(HarvestViewerContext viewer,
            String buruhId,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            Pageable pageable) {
        validateViewerRole(viewer, "MANDOR", "ADMIN");
        validateDateRange(startDate, endDate);
        HarvestStatus parsedStatus = parseStatus(status);
        validatePageSize(pageable);

        BuruhMandorAssignment targetAssignment = assignmentRepository.findByBuruhId(buruhId)
                .orElseGet(() -> {
                    if (assignmentRepository.existsByMandorId(buruhId)) {
                        throw new HarvestValidationException(HarvestErrorKey.USER_NOT_BURUH,
                                "The specified user exists but is not BURUH");
                    }
                    throw new HarvestNotFoundException(HarvestErrorKey.USER_NOT_FOUND,
                            "No user found with the given buruhId");
                });

        if (viewer.role().equals("MANDOR") && !viewer.userId().equals(targetAssignment.getMandorId())) {
            throw new HarvestAuthorizationException(HarvestErrorKey.MANDOR_NOT_AUTHORIZED,
                    "Authenticated mandor does not supervise this buruh");
        }

        Specification<Harvest> spec = buildCommonFilters(startDate, endDate, parsedStatus, null)
                .and((root, query, cb) -> cb.equal(root.get("buruhId"), buruhId));

        return toPageResponse(harvestRepository.findAll(spec, pageable));
    }

    private void validateViewerRole(HarvestViewerContext viewer, String... allowedRoles) {
        if (viewer == null || viewer.role() == null || viewer.userId() == null || viewer.userId().isBlank()) {
            throw new HarvestAuthorizationException("Invalid authenticated user context");
        }

        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(viewer.role())) {
                return;
            }
        }

        throw new HarvestAuthorizationException(HarvestErrorKey.FORBIDDEN, "Caller role is not allowed for this endpoint");
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new HarvestValidationException(HarvestErrorKey.INVALID_DATE_RANGE,
                    "startDate cannot be after endDate");
        }
    }

    private HarvestStatus parseStatus(String status) {
        String normalized = normalizeString(status);
        if (normalized == null) {
            return null;
        }

        try {
            return HarvestStatus.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new HarvestValidationException(HarvestErrorKey.INVALID_STATUS_VALUE,
                    "status must be one of PENDING, APPROVED, REJECTED");
        }
    }

    private void validatePageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new HarvestValidationException("size must be <= 100");
        }
    }

    private Specification<Harvest> buildCommonFilters(LocalDate startDate,
            LocalDate endDate,
            HarvestStatus status,
            String buruhName) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("harvestDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("harvestDate"), endDate));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (buruhName != null) {
                predicates.add(cb.like(cb.lower(root.get("buruhName")), "%" + buruhName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String normalizeString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private HarvestPageResponse toPageResponse(Page<Harvest> page) {
        List<HarvestResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new HarvestPageResponse(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private HarvestResponse toResponse(Harvest harvest) {
        HarvestResponse response = new HarvestResponse();
        response.setId(harvest.getId());
        response.setBuruhId(harvest.getBuruhId());
        response.setBuruhName(harvest.getBuruhName());
        response.setWeightKg(harvest.getWeightKg());
        response.setNotes(harvest.getNotes());
        response.setStatus(harvest.getStatus());
        response.setRejectionReason(harvest.getRejectionReason());
        response.setHarvestDate(harvest.getHarvestDate());
        response.setCreatedAt(harvest.getCreatedAt());
        response.setReviewedAt(harvest.getApprovedAt());
        response.setPhotoUrls(harvest.getPhotos().stream().map(photo -> photo.getPhotoUrl()).toList());
        return response;
    }
}