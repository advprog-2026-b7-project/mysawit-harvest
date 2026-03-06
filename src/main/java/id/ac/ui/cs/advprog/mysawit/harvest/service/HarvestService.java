package id.ac.ui.cs.advprog.mysawit.harvest.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestCreateRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAlreadyExistsException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestStorageException;
import id.ac.ui.cs.advprog.mysawit.harvest.model.Harvest;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestPhoto;
import id.ac.ui.cs.advprog.mysawit.harvest.model.HarvestStatus;
import id.ac.ui.cs.advprog.mysawit.harvest.repository.HarvestRepository;

@Service
public class HarvestService {

    private final HarvestRepository harvestRepository;
    private final Path storageRoot;

    public HarvestService(HarvestRepository harvestRepository,
            @Value("${storage.harvest.dir:uploads/harvests}") String storageDir) {
        this.harvestRepository = harvestRepository;
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @Transactional
    public HarvestResponse createHarvest(HarvestCreateRequest request, List<MultipartFile> photos) {
        ensureUniqueForToday(request.getBuruhId());

        Harvest harvest = new Harvest();
        harvest.setPlantationId(request.getPlantationId());
        harvest.setBuruhId(request.getBuruhId());
        harvest.setWeightKg(request.getWeightKg());
        harvest.setDescription(request.getDescription());
        harvest.setStatus(HarvestStatus.SUBMITTED);

        if (photos != null) {
            photos.stream()
                    .filter(photo -> !photo.isEmpty())
                    .forEach(photo -> harvest.addPhoto(buildPhotoEntity(photo)));
        }

        Harvest saved = harvestRepository.save(harvest);
        return toResponse(saved);
    }

    private void ensureUniqueForToday(String buruhId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        boolean exists = harvestRepository.existsByBuruhIdAndCreatedAtBetween(buruhId, start, end);
        if (exists) {
            throw new HarvestAlreadyExistsException("Harvest for today already submitted");
        }
    }

    private HarvestPhoto buildPhotoEntity(MultipartFile file) {
        try {
            Files.createDirectories(storageRoot);
            String originalName = file.getOriginalFilename();
            String extension = (originalName != null && originalName.contains("."))
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : "";
            String filename = UUID.randomUUID() + extension;

            Path target = storageRoot.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            HarvestPhoto photo = new HarvestPhoto();
            photo.setPhotoUrl(target.toString());
            return photo;
        } catch (IOException ex) {
            throw new HarvestStorageException("Failed to store harvest photo", ex);
        }
    }

    private HarvestResponse toResponse(Harvest harvest) {
        HarvestResponse response = new HarvestResponse();
        response.setId(harvest.getId());
        response.setPlantationId(harvest.getPlantationId());
        response.setBuruhId(harvest.getBuruhId());
        response.setWeightKg(harvest.getWeightKg());
        response.setDescription(harvest.getDescription());
        response.setStatus(harvest.getStatus());
        response.setRejectionReason(harvest.getRejectionReason());
        response.setApprovedAt(harvest.getApprovedAt());
        response.setCreatedAt(harvest.getCreatedAt());

        List<String> photoUrls = new ArrayList<>();
        harvest.getPhotos().forEach(photo -> photoUrls.add(photo.getPhotoUrl()));
        response.setPhotoUrls(photoUrls);
        return response;
    }
}