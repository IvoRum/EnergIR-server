package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackResponse {
    private Integer id;
    private String type;
    private String title;
    private String description;
    private String email;
    private Integer rating;
    private String userEmail;
    private String userAgent;
    private String url;
    private Instant createdOn;
}
