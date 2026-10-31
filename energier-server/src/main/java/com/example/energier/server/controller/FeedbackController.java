package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateFeedbackRequest;
import com.example.energier.server.model.responce.FeedbackResponse;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<FeedbackResponse> submit(
            Authentication authentication,
            @RequestBody CreateFeedbackRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(feedbackService.submit(companyId, authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponse>> getMine(Authentication authentication) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(feedbackService.getAll(companyId));
    }
}
