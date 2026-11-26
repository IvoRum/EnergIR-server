package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolarReadingResponse {
    private Long id;
    private Integer panelId;
    private double powerOutput;
    private String unit;
    private Double irradiance;
    /** yyyy-MM-dd */
    private String date;
    /** epoch milliseconds */
    private long timestamp;
}
