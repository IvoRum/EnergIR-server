package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateElectricMeterEnergyReadingRequest;
import com.example.energier.server.model.responce.ElectricMeterEnergyReadingResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.ElectricMeterEnergyReadingService;
import com.example.energier.server.service.ElectricMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ElectricMeterEnergyReadingController {

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 1000;

    private final ElectricMeterEnergyReadingService readingService;
    private final ElectricMeterService electricMeterService;
    private final CompanyService companyService;

    @PostMapping("/api/v1/readings")
    public ResponseEntity<ElectricMeterEnergyReadingResponse> create(
            @RequestBody CreateElectricMeterEnergyReadingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(readingService.create(request));
    }

    @GetMapping("/api/v1/meters/{meterId}/readings")
    public ResponseEntity<List<ElectricMeterEnergyReadingResponse>> getHistory(
            Authentication authentication,
            @PathVariable Integer meterId,
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false) Integer limit
    ) {
        // Ensure the meter belongs to the caller's company (404 otherwise).
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        electricMeterService.getById(companyId, meterId);

        int effectiveLimit = limit == null ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return ResponseEntity.ok(readingService.getHistory(meterId, since, effectiveLimit));
    }
}
