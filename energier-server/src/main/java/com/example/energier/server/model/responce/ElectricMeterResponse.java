package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ElectricMeterResponse {
    private Integer id;
    private String edgeId;
    private Integer edgeMeterId;
    private Integer address;
    private String name;
    private String manufacturer;
    private String model;
    private boolean active;
    private Instant createdOn;

    /** Latest reading for this meter, or null when it has never reported. */
    private ElectricMeterEnergyReadingResponse latestReading;

    /** Average daily consumption from reading deltas over the last 30 days. */
    private double dailyAverage;

    /** Consumption per day for the last 7 days, derived from reading deltas. */
    private List<WeeklyConsumptionResponse> weeklyConsumption;
}
