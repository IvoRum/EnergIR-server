package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateElectricMeterRequest {
    private String name;
    private Integer address;
    private String manufacturer;
    private String model;
    private Boolean active;
}
