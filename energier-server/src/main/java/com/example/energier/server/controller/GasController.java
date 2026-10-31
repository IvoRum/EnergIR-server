package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateGasGroupRequest;
import com.example.energier.server.model.request.CreateGasMeterRequest;
import com.example.energier.server.model.request.CreateGasReadingRequest;
import com.example.energier.server.model.request.UpdateGasMeterRequest;
import com.example.energier.server.model.responce.GasGroupResponse;
import com.example.energier.server.model.responce.GasMeterResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.GasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gas module, scoped to the authenticated user's company. */
@RestController
@RequestMapping("/api/v1/gas")
@RequiredArgsConstructor
public class GasController {

    private final GasService gasService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<GasMeterResponse>> getAll(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(gasService.getAll(companyId));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<GasGroupResponse>> getGroups(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(gasService.getGroups(companyId));
    }

    @PostMapping("/groups")
    public ResponseEntity<GasGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateGasGroupRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gasService.createGroup(companyId, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        gasService.deleteGroup(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GasMeterResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(gasService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<GasMeterResponse> create(
            Authentication authentication,
            @RequestBody CreateGasMeterRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gasService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GasMeterResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateGasMeterRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(gasService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        gasService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/data")
    public ResponseEntity<GasMeterResponse> addReading(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody CreateGasReadingRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(gasService.addReading(companyId, id, request));
    }

    @DeleteMapping("/{id}/data/{readingId}")
    public ResponseEntity<Void> deleteReading(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long readingId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        gasService.deleteReading(companyId, id, readingId);
        return ResponseEntity.noContent().build();
    }
}
