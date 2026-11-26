package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompressorReadingResponse {
    private Long id;
    private double quantity;
    private String unit;
    private String date;
    private String time;
}
