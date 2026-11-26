package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Edits a solar panel. Send the fields you want to change. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSolarPanelRequest {
    private String name;
    private String description;
    private String location;
    private Integer groupId;
    private String unit;
    private String status;
    private String manufacturer;
    /** yyyy-MM-dd */
    private String installDate;
    private Double capacity;
    private Double efficiency;
    private Double currentOutput;
}
