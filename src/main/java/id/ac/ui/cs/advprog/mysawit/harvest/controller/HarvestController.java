package id.ac.ui.cs.advprog.mysawit.harvest.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApiSuccessResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApproveHarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestCreateRequest;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestJwtClaimsResolver;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestHistoryService;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestService;
import jakarta.validation.Valid;

@CrossOrigin(origins = "http://localhost:3001")
@RestController
@Validated
@RequestMapping("/api/v1/harvests")
public class HarvestController {

    private final HarvestService harvestService;
    private final HarvestHistoryService harvestHistoryService;
    private final HarvestJwtClaimsResolver claimsResolver;

    public HarvestController(HarvestService harvestService,
            HarvestHistoryService harvestHistoryService,
            HarvestJwtClaimsResolver claimsResolver) {
        this.harvestService = harvestService;
        this.harvestHistoryService = harvestHistoryService;
        this.claimsResolver = claimsResolver;
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<HarvestResponse>> createHarvest(
            @Valid @ModelAttribute HarvestCreateRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        HarvestResponse response = harvestService.createHarvest(
                request,
                claimsResolver.resolve(authorization),
                photos);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiSuccessResponse<>("success", response));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<HarvestPageResponse>> getHarvestHistory(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String buruhName,
            @PageableDefault(size = 20) Pageable pageable) {
        HarvestPageResponse response = harvestHistoryService.getHarvestHistory(
                claimsResolver.resolveViewer(authorization),
                startDate,
                endDate,
                status,
                buruhName,
                pageable);

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }

    @PatchMapping(value = "/{harvestId}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<ApproveHarvestResponse>> approveHarvest(
            @PathVariable UUID harvestId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ApproveHarvestResponse response = harvestService.approveHarvest(
                harvestId,
                claimsResolver.resolveMandor(authorization));

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }
}
