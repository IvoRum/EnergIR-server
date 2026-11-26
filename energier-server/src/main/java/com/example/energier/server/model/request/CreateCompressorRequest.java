package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCompressorRequest {
    private String name;
    private String description;
    /** Optional: the electric meter feeding this compressor; null = none. */
    private Integer electricMeterId;
}
