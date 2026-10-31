package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateElectricMeterRequest;
import com.example.energier.server.model.request.UpdateElectricMeterRequest;
import com.example.energier.server.model.responce.ElectricMeterResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.ElectricMeterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * All meter operations are scoped to the authenticated user's company. The
 * company id is resolved server-side from the JWT, never from the client.
 */
@RestController
@RequestMapping("/api/v1/meters")
@RequiredArgsConstructor
public class ElectricMeterController {

    private final ElectricMeterService electricMeterService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<ElectricMeterResponse>> getAll(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(electricMeterService.getAll(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ElectricMeterResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(electricMeterService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<ElectricMeterResponse> create(
            Authentication authentication,
            @RequestBody CreateElectricMeterRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(electricMeterService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ElectricMeterResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateElectricMeterRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(electricMeterService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        electricMeterService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }
}
