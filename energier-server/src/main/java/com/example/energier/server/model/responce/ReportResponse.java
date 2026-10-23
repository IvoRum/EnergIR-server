package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Complete report payload for the authenticated user's company. The web client
 * filters this by date range, selected meters/products and chosen columns.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportResponse {

    private Instant generatedAt;

    private List<EnergyMeter> energy;
    private List<WaterMeter> water;
    private List<GasMeter> gas;
    private List<ProductionProduct> production;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EnergyMeter {
        private Integer id;
        private String name;
        private String location;
        private String groupName;
        private String unit;
        private List<EnergyReading> readings;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EnergyReading {
        private Long id;
        private Instant recordedOn;
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
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WaterMeter {
        private Integer id;
        private String name;
        private String location;
        private String groupName;
        private String unit;
        private double currentValue;
        private double monthlyTotal;
        private double dailyAverage;
        private List<WaterReading> readings;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WaterReading {
        private Long id;
        private double value;
        private Instant recordedOn;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GasMeter {
        private Integer id;
        private String name;
        private String location;
        private String groupName;
        private String unit;
        private double currentValue;
        private double monthlyTotal;
        private double dailyAverage;
        private List<GasReading> readings;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GasReading {
        private Long id;
        private double value;
        private Instant recordedOn;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductionProduct {
        private Integer id;
        private String name;
        private String groupName;
        private String unit;
        private double totalProduced;
        private List<ProductionRecord> records;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductionRecord {
        private Long id;
        private double quantity;
        private String quality;
        private Instant recordedOn;
    }
}
