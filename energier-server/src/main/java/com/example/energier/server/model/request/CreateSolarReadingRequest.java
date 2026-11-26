package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateSolarReadingRequest {
    /** Power output at the recorded time, in the panel's unit. */
    private Double powerOutput;
    private String unit;
    /** W/m² (optional). */
    private Double irradiance;
    /** yyyy-MM-dd */
    private String date;
    /** HH:mm (optional) */
    private String time;
}
