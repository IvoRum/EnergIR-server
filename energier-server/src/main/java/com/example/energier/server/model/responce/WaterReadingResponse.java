package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WaterReadingResponse {
    private Long id;
    private Integer meterId;
    private double value;
    private String unit;
    /** yyyy-MM-dd */
    private String date;
    /** epoch milliseconds */
    private long timestamp;
}
