package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCompressorRequest {
    private String name;
    private String description;
    /** Send 0 (or omit) to detach the meter; null keeps the current one. */
    private Integer electricMeterId;
}
