package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateCompanyRequest;
import com.example.energier.server.model.responce.CompanyResponse;
import com.example.energier.server.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    /** Companies the current user belongs to (0, 1 or many). */
    @GetMapping
    public ResponseEntity<List<CompanyResponse>> getMine(Authentication authentication) {
        return ResponseEntity.ok(companyService.getCompaniesForUser(authentication.getName()));
    }

    /** Creates a company and links it to the current user. */
    @PostMapping
    public ResponseEntity<CompanyResponse> create(
            Authentication authentication,
            @RequestBody CreateCompanyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyService.createCompanyForUser(authentication.getName(), request.getName()));
    }
}
