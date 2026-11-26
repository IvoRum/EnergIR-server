package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolarPanelResponse {
    private Integer id;
    private boolean archived;
    private String name;
    private String description;
    private String location;
    private String status;
    private Integer groupId;
    private double currentOutput;
    private String unit;
    /** Human readable, e.g. "5 min ago" or "No readings". */
    private String lastUpdated;
    /** Power output per day for the last 7 days, oldest first. */
    private java.util.List<Double> weeklyGeneration;
    private double monthlyTotal;
    private double dailyAverage;
    private double capacity;
    private double efficiency;
    private String installDate;
    private String manufacturer;
    private java.util.List<SolarReadingResponse> readings;
}
