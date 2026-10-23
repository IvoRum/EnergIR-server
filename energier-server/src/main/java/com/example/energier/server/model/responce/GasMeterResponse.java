package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GasMeterResponse {
    private Integer id;
    private String name;
    private String description;
    private String location;
    private String status;
    private Integer groupId;
    private double currentValue;
    private String unit;
    /** Human readable, e.g. "5 min ago" or "No readings". */
    private String lastUpdated;
    private List<Double> weeklyUsage;
    private double monthlyTotal;
    private double dailyAverage;
    private String installDate;
    private String manufacturer;
    private List<GasReadingResponse> readings;
}
