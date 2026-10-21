package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateFeedbackRequest;
import com.example.energier.server.model.responce.FeedbackResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.Feedback;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class FeedbackService {

    private static final Set<String> TYPES = Set.of("bug", "feature", "improvement", "other");

    private final FeedbackRepository feedbackRepository;
    private final CompanyRepository companyRepository;

    public FeedbackResponse submit(Integer companyId, String userEmail, CreateFeedbackRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        String title = requireText(request.getTitle(), "Title");
        String description = requireText(request.getDescription(), "Description");

        Integer rating = request.getRating();
        if (rating != null && (rating < 1 || rating > 5)) {
            rating = null;
        }

        Feedback feedback = Feedback.builder()
                .company(company)
                .userEmail(userEmail)
                .type(normalizeType(request.getType()))
                .title(title)
                .description(description)
                .email(trimToNull(request.getEmail()))
                .rating(rating)
                .clientTimestamp(request.getTimestamp())
                .userAgent(truncate(request.getUserAgent(), 1000))
                .url(truncate(request.getUrl(), 1000))
                .createdOn(Instant.now())
                .build();

        return toResponse(feedbackRepository.save(feedback));
    }

    public List<FeedbackResponse> getAll(Integer companyId) {
        return feedbackRepository.findAllByCompany_IdOrderByCreatedOnDesc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase();
        return TYPES.contains(normalized) ? normalized : "other";
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .type(feedback.getType())
                .title(feedback.getTitle())
                .description(feedback.getDescription())
                .email(feedback.getEmail())
                .rating(feedback.getRating())
                .userEmail(feedback.getUserEmail())
                .userAgent(feedback.getUserAgent())
                .url(feedback.getUrl())
                .createdOn(feedback.getCreatedOn())
                .build();
    }
}
