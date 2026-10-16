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
}
