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
public class ElectricMeterEnergyReadingResponse {
    private Long id;
    private Integer meterId;
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
