package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "electric_meter_energy_reading", uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "created_on"}))
public class ElectricMeterEnergyReading {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private ElectricMeter meter;

    @Column(name = "TotalActiveEnergyImportTariff1", nullable = false)
    private double totalActiveEnergyImportTariff1;

    @Column(name = "TotalActiveEnergyImportTariff2", nullable = false)
    private double totalActiveEnergyImportTariff2;

    @Column(name = "TotalActiveEnergyExportTariff1", nullable = false)
    private double totalActiveEnergyExportTariff1;

    @Column(name = "TotalActiveEnergyExportTariff2", nullable = false)
    private double totalActiveEnergyExportTariff2;

    @Column(name = "TotalReactiveEnergyImportTariff1", nullable = false)
    private double totalReactiveEnergyImportTariff1;

    @Column(name = "TotalReactiveEnergyImportTariff2", nullable = false)
    private double totalReactiveEnergyImportTariff2;

    @Column(name = "TotalReactiveEnergyExportTariff1", nullable = false)
    private double totalReactiveEnergyExportTariff1;

    @Column(name = "TotalReactiveEnergyExportTariff2", nullable = false)
    private double totalReactiveEnergyExportTariff2;

    @Column(name = "TotalApparentEnergyTariff1", nullable = false)
    private double totalApparentEnergyTariff1;

    @Column(name = "TotalApparentEnergyTariff2", nullable = false)
    private double totalApparentEnergyTariff2;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
