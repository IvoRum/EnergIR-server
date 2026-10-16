package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateElectricMeterRequest {
    private String edgeId;
    private Integer edgeMeterId;
    private Integer address;
    private String name;
    private String manufacturer;
    private String model;
}
