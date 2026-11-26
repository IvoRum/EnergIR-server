package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateSolarGroupRequest;
import com.example.energier.server.model.request.CreateSolarPanelRequest;
import com.example.energier.server.model.request.CreateSolarReadingRequest;
import com.example.energier.server.model.request.UpdateSolarGroupRequest;
import com.example.energier.server.model.request.UpdateSolarPanelRequest;
import com.example.energier.server.model.request.UpdateSolarReadingRequest;
import com.example.energier.server.model.responce.SolarGroupResponse;
import com.example.energier.server.model.responce.SolarPanelResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.SolarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Solar module, scoped to the authenticated user's company. */
@RestController
@RequestMapping("/api/v1/solar")
@RequiredArgsConstructor
public class SolarController {

    private final SolarService solarService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<SolarPanelResponse>> getAll(
            Authentication authentication,
            @RequestParam(name = "archived", defaultValue = "false") boolean archived
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.getAll(companyId, archived));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<SolarGroupResponse>> getGroups(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.getGroups(companyId));
    }

    @PostMapping("/groups")
    public ResponseEntity<SolarGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateSolarGroupRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solarService.createGroup(companyId, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        solarService.deleteGroup(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolarPanelResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<SolarPanelResponse> create(
            Authentication authentication,
            @RequestBody CreateSolarPanelRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(solarService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SolarPanelResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateSolarPanelRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        solarService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    /** Soft delete: keeps the panel and its readings, hides it from the active list. */
    @PutMapping("/{id}/archive")
    public ResponseEntity<SolarPanelResponse> archive(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.setArchived(companyId, id, true));
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<SolarPanelResponse> unarchive(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.setArchived(companyId, id, false));
    }

    @PostMapping("/{id}/data")
    public ResponseEntity<SolarPanelResponse> addReading(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody CreateSolarReadingRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.addReading(companyId, id, request));
    }

    @DeleteMapping("/{id}/data/{readingId}")
    public ResponseEntity<Void> deleteReading(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long readingId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        solarService.deleteReading(companyId, id, readingId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/data/{readingId}")
    public ResponseEntity<SolarPanelResponse> updateReading(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long readingId,
            @RequestBody UpdateSolarReadingRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.updateReading(companyId, id, readingId, request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<SolarGroupResponse> updateGroup(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateSolarGroupRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(solarService.updateGroup(companyId, id, request));
    }
}
