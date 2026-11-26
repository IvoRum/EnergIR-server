package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Edits one stored solar reading. Send the fields you want to change. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSolarReadingRequest {
    private Double powerOutput;
    private String unit;
    private Double irradiance;
    private String date;
    private String time;
}
