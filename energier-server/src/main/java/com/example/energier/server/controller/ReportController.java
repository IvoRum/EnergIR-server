package com.example.energier.server.controller;

import com.example.energier.server.model.responce.ReportResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Reports for the authenticated user's company. */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<ReportResponse> getReports(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(reportService.getReports(companyId));
    }
}
