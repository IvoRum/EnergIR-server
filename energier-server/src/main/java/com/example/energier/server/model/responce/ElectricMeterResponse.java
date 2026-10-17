package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
}
