package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateWaterGroupRequest;
import com.example.energier.server.model.request.CreateWaterMeterRequest;
import com.example.energier.server.model.request.CreateWaterReadingRequest;
import com.example.energier.server.model.request.UpdateWaterMeterRequest;
import com.example.energier.server.model.responce.WaterGroupResponse;
import com.example.energier.server.model.responce.WaterMeterResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.WaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Water module, scoped to the authenticated user's company. */
@RestController
@RequestMapping("/api/v1/water")
@RequiredArgsConstructor
public class WaterController {

    private final WaterService waterService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<WaterMeterResponse>> getAll(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(waterService.getAll(companyId));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<WaterGroupResponse>> getGroups(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(waterService.getGroups(companyId));
    }

    @PostMapping("/groups")
    public ResponseEntity<WaterGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateWaterGroupRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waterService.createGroup(companyId, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        waterService.deleteGroup(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<WaterMeterResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(waterService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<WaterMeterResponse> create(
            Authentication authentication,
            @RequestBody CreateWaterMeterRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(waterService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WaterMeterResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateWaterMeterRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(waterService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        waterService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/data")
    public ResponseEntity<WaterMeterResponse> addReading(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody CreateWaterReadingRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(waterService.addReading(companyId, id, request));
    }

    @DeleteMapping("/{id}/data/{readingId}")
    public ResponseEntity<Void> deleteReading(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long readingId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        waterService.deleteReading(companyId, id, readingId);
        return ResponseEntity.noContent().build();
    }
}
