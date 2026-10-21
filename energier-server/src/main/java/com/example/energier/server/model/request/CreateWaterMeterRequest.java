package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateWaterMeterRequest {
    private String name;
    private String description;
    private String location;
    private Integer groupId;
    private String unit;
    private String manufacturer;
    /** yyyy-MM-dd */
    private String installDate;
    private Double currentValue;
}
