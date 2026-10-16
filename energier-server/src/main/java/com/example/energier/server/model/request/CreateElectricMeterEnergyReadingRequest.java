package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateElectricMeterEnergyReadingRequest {
    private String edgeId;
    private Integer edgeMeterId;
    private double totalActiveEnergyImportTariff1;
    private double totalActiveEnergyImportTariff2;
    private double totalActiveEnergyExportTariff1;
    private double totalActiveEnergyExportTariff2;
    private double totalReactiveEnergyImportTariff1;
    private double totalReactiveEnergyImportTariff2;
    private double totalReactiveEnergyExportTariff1;
    private double totalReactiveEnergyExportTariff2;
    private double totalApparentEnergyTariff1;
    private double totalApparentEnergyTariff2;
    private Instant createdOn;
}
