package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Edits one stored electric energy reading. Every counter is optional: fields
 * left null keep their stored value, which matters because the counters are
 * cumulative totals rather than deltas.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateElectricMeterEnergyReadingRequest {
    private Double totalActiveEnergyImportTariff1;
    private Double totalActiveEnergyImportTariff2;
    private Double totalActiveEnergyExportTariff1;
    private Double totalActiveEnergyExportTariff2;
    private Double totalReactiveEnergyImportTariff1;
    private Double totalReactiveEnergyImportTariff2;
    private Double totalReactiveEnergyExportTariff1;
    private Double totalReactiveEnergyExportTariff2;
    private Double totalApparentEnergyTariff1;
    private Double totalApparentEnergyTariff2;
    private Instant createdOn;
}
