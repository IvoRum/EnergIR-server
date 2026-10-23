package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeeklyConsumptionResponse {
    private String day;
    /** yyyy-MM-dd */
    private String date;
    private double consumption;
}
