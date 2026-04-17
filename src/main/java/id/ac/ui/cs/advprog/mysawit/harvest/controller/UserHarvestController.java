package id.ac.ui.cs.advprog.mysawit.harvest.controller;

import java.time.LocalDate;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.ac.ui.cs.advprog.mysawit.harvest.dto.ApiSuccessResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.dto.HarvestPageResponse;
import id.ac.ui.cs.advprog.mysawit.harvest.security.HarvestJwtClaimsResolver;
import id.ac.ui.cs.advprog.mysawit.harvest.service.HarvestHistoryService;

@CrossOrigin(origins = "http://localhost:3001")
@RestController
@Validated
@RequestMapping("/api/v1/users")
public class UserHarvestController {

    private final HarvestHistoryService harvestHistoryService;
    private final HarvestJwtClaimsResolver claimsResolver;

    public UserHarvestController(HarvestHistoryService harvestHistoryService, HarvestJwtClaimsResolver claimsResolver) {
        this.harvestHistoryService = harvestHistoryService;
        this.claimsResolver = claimsResolver;
    }

    @GetMapping(value = "/{buruhId}/harvests", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<HarvestPageResponse>> getHarvestHistoryByBuruhId(
            @PathVariable String buruhId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        HarvestPageResponse response = harvestHistoryService.getHarvestHistoryByBuruhId(
                claimsResolver.resolveViewer(authorization),
                buruhId,
                startDate,
                endDate,
                status,
                pageable);

        return ResponseEntity.ok(new ApiSuccessResponse<>("success", response));
    }
}