package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateProductGroupRequest;
import com.example.energier.server.model.request.CreateProductRequest;
import com.example.energier.server.model.request.CreateProductionRecordRequest;
import com.example.energier.server.model.request.UpdateProductRequest;
import com.example.energier.server.model.responce.ProductGroupResponse;
import com.example.energier.server.model.responce.ProductResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Production module, scoped to the authenticated user's company. */
@RestController
@RequestMapping("/api/v1/production")
@RequiredArgsConstructor
public class ProductionController {

    private final ProductionService productionService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(productionService.getAll(companyId));
    }

    @GetMapping("/groups")
    public ResponseEntity<List<ProductGroupResponse>> getGroups(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(productionService.getGroups(companyId));
    }

    @PostMapping("/groups")
    public ResponseEntity<ProductGroupResponse> createGroup(
            Authentication authentication,
            @RequestBody CreateProductGroupRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productionService.createGroup(companyId, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        productionService.deleteGroup(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(productionService.getById(companyId, id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            Authentication authentication,
            @RequestBody CreateProductRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productionService.create(companyId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody UpdateProductRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(productionService.update(companyId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        productionService.delete(companyId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/data")
    public ResponseEntity<ProductResponse> addData(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody CreateProductionRecordRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(productionService.addRecord(companyId, id, request));
    }

    @DeleteMapping("/{id}/data/{recordId}")
    public ResponseEntity<Void> deleteData(
            Authentication authentication,
            @PathVariable Integer id,
            @PathVariable Long recordId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        productionService.deleteRecord(companyId, id, recordId);
        return ResponseEntity.noContent().build();
    }
}
