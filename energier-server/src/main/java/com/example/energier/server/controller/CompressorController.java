package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateCompressorReadingRequest;
import com.example.energier.server.model.request.CreateCompressorRequest;
import com.example.energier.server.model.request.UpdateCompressorRequest;
import com.example.energier.server.model.responce.CompressorResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.CompressorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Compressors and their air output (cubic metres), scoped to the caller's company. */
@RestController
@RequestMapping("/api/v1/compressors")
@RequiredArgsConstructor
public class CompressorController {

    private final CompressorService compressorService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<CompressorResponse>> getAll(
            Authentication authentication,
            @RequestParam(name = "archived", defaultValue = "false") boolean archived
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(compressorService.getAll(companyId, archived));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompressorResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(compressorService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<CompressorResponse> create(
            Authentication authentication,
            @RequestBody CreateCompressorRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(compressorService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompressorResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateCompressorRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(compressorService.update(companyId, id, request));
    }

    /** Soft delete: keeps the compressor and its readings, hides it from the list. */
    @PutMapping("/{id}/archive")
    public ResponseEntity<CompressorResponse> archive(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(compressorService.setArchived(companyId, id, true));
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<CompressorResponse> unarchive(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(compressorService.setArchived(companyId, id, false));
    }

    /** Hard delete: removes the compressor together with its readings. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        compressorService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/data")
    public ResponseEntity<CompressorResponse> addReading(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody CreateCompressorReadingRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(compressorService.addReading(companyId, id, request));
    }

    @PutMapping("/{id}/data/{readingId}")
    public ResponseEntity<CompressorResponse> updateReading(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long readingId,
            @RequestBody CreateCompressorReadingRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(compressorService.updateReading(companyId, id, readingId, request));
    }

    @DeleteMapping("/{id}/data/{readingId}")
    public ResponseEntity<Void> deleteReading(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long readingId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        compressorService.deleteReading(companyId, id, readingId);
        return ResponseEntity.noContent().build();
    }
}
