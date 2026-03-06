package id.ac.ui.cs.advprog.mysawit.harvest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestCreateRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestAlreadyExistsException;
import id.ac.ui.cs.advprog.mysawit.harvest.exception.HarvestStorageException;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3001")
@RestController
@RequestMapping("/harvests")
public class HarvestController {

    private final HarvestService harvestService;

    public HarvestController(HarvestService harvestService) {
        this.harvestService = harvestService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HarvestResponse> createHarvest(
            @Valid @RequestPart("data") HarvestCreateRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        HarvestResponse response = harvestService.createHarvest(request, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(HarvestAlreadyExistsException.class)
    public ResponseEntity<String> handleDuplicate(HarvestAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(HarvestStorageException.class)
    public ResponseEntity<String> handleStorage(HarvestStorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }
}