package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateGasReadingRequest {
    private Double value;
    private String unit;
    /** yyyy-MM-dd */
    private String date;
    /** HH:mm (optional) */
    private String time;
}
