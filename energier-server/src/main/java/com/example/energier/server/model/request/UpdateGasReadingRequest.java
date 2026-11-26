package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Edits one stored gas reading. Send the fields you want to change. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGasReadingRequest {
    private Double value;
    private String unit;
    private String date;
    private String time;
}
