package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateFeedbackRequest {
    private String type;
    private String title;
    private String description;
    private String email;
    private Integer rating;
    private Long timestamp;
    private String userAgent;
    private String url;
}
