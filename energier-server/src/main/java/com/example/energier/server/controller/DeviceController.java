package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateDeviceRequest;
import com.example.energier.server.model.request.HeartbeatRequest;
import com.example.energier.server.model.request.UpdateDeviceRequest;
import com.example.energier.server.model.responce.DeviceResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Edge devices, always scoped to the authenticated user's company.
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAll(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(deviceService.getAll(companyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(deviceService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> create(
            Authentication authentication,
            @RequestBody CreateDeviceRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.create(companyId, request));
    }

    /** Edge liveness check: updates lastSeen and derives online/offline. */
    @PostMapping("/heartbeat")
    public ResponseEntity<DeviceResponse> heartbeat(
            Authentication authentication,
            @RequestBody HeartbeatRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(deviceService.heartbeat(companyId, request.getCode()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateDeviceRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(deviceService.update(companyId, id, request));
    }
}
