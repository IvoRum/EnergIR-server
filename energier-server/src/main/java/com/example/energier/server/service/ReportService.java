package com.example.energier.server.service;

import com.example.energier.server.model.responce.ReportResponse;
import com.example.energier.server.persistence.GasMeter;
import com.example.energier.server.persistence.GasReading;
import com.example.energier.server.persistence.ProductionRecord;
import com.example.energier.server.persistence.WaterMeter;
import com.example.energier.server.persistence.WaterReading;
import com.example.energier.server.repository.ElectricMeterEnergyReadingRepository;
import com.example.energier.server.repository.ElectricMeterRepository;
import com.example.energier.server.repository.GasMeterRepository;
import com.example.energier.server.repository.GasReadingRepository;
import com.example.energier.server.repository.ProductRepository;
import com.example.energier.server.repository.ProductionRecordRepository;
import com.example.energier.server.repository.WaterMeterRepository;
import com.example.energier.server.repository.WaterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final ElectricMeterRepository electricMeterRepository;
    private final ElectricMeterEnergyReadingRepository electricReadingRepository;
    private final WaterMeterRepository waterMeterRepository;
    private final WaterReadingRepository waterReadingRepository;
    private final GasMeterRepository gasMeterRepository;
    private final GasReadingRepository gasReadingRepository;
    private final ProductRepository productRepository;
    private final ProductionRecordRepository productionRecordRepository;

    public ReportResponse getReports(Integer companyId) {
        return ReportResponse.builder()
                .generatedAt(Instant.now())
                .energy(buildEnergy(companyId))
                .water(buildWater(companyId))
                .gas(buildGas(companyId))
                .production(buildProduction(companyId))
                .build();
    }

    private List<ReportResponse.EnergyMeter> buildEnergy(Integer companyId) {
        return electricMeterRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(meter -> ReportResponse.EnergyMeter.builder()
                        .id(meter.getId())
                        .name(meter.getName())
                        .location(meter.getAddress() != null ? String.valueOf(meter.getAddress()) : null)
                        .groupName(null)
                        .unit("kWh")
                        .readings(electricReadingRepository.findByMeter_IdOrderByCreatedOnAsc(meter.getId()).stream()
                                .map(reading -> ReportResponse.EnergyReading.builder()
                                        .id(reading.getId())
                                        .recordedOn(reading.getCreatedOn())
                                        .totalActiveEnergyImportTariff1(reading.getTotalActiveEnergyImportTariff1())
                                        .totalActiveEnergyImportTariff2(reading.getTotalActiveEnergyImportTariff2())
                                        .totalActiveEnergyExportTariff1(reading.getTotalActiveEnergyExportTariff1())
                                        .totalActiveEnergyExportTariff2(reading.getTotalActiveEnergyExportTariff2())
                                        .totalReactiveEnergyImportTariff1(reading.getTotalReactiveEnergyImportTariff1())
                                        .totalReactiveEnergyImportTariff2(reading.getTotalReactiveEnergyImportTariff2())
                                        .totalReactiveEnergyExportTariff1(reading.getTotalReactiveEnergyExportTariff1())
                                        .totalReactiveEnergyExportTariff2(reading.getTotalReactiveEnergyExportTariff2())
                                        .totalApparentEnergyTariff1(reading.getTotalApparentEnergyTariff1())
                                        .totalApparentEnergyTariff2(reading.getTotalApparentEnergyTariff2())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private List<ReportResponse.WaterMeter> buildWater(Integer companyId) {
        Instant monthAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        return waterMeterRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(meter -> {
                    List<WaterReading> readings =
                            waterReadingRepository.findByMeter_IdOrderByRecordedOnAsc(meter.getId());
                    double monthlyTotal = readings.stream()
                            .filter(reading -> !reading.getRecordedOn().isBefore(monthAgo))
                            .mapToDouble(WaterReading::getValue)
                            .sum();

                    return ReportResponse.WaterMeter.builder()
                            .id(meter.getId())
                            .name(meter.getName())
                            .location(meter.getLocation())
                            .groupName(meter.getGroup() != null ? meter.getGroup().getName() : null)
                            .unit(meter.getUnit())
                            .currentValue(meter.getCurrentValue())
                            .monthlyTotal(monthlyTotal)
                            .dailyAverage(monthlyTotal / 30.0)
                            .readings(readings.stream()
                                    .map(reading -> ReportResponse.WaterReading.builder()
                                            .id(reading.getId())
                                            .value(reading.getValue())
                                            .recordedOn(reading.getRecordedOn())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    private List<ReportResponse.GasMeter> buildGas(Integer companyId) {
        Instant monthAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        return gasMeterRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(meter -> {
                    List<GasReading> readings =
                            gasReadingRepository.findByMeter_IdOrderByRecordedOnAsc(meter.getId());
                    double monthlyTotal = readings.stream()
                            .filter(reading -> !reading.getRecordedOn().isBefore(monthAgo))
                            .mapToDouble(GasReading::getValue)
                            .sum();

                    return ReportResponse.GasMeter.builder()
                            .id(meter.getId())
                            .name(meter.getName())
                            .location(meter.getLocation())
                            .groupName(meter.getGroup() != null ? meter.getGroup().getName() : null)
                            .unit(meter.getUnit())
                            .currentValue(meter.getCurrentValue())
                            .monthlyTotal(monthlyTotal)
                            .dailyAverage(monthlyTotal / 30.0)
                            .readings(readings.stream()
                                    .map(reading -> ReportResponse.GasReading.builder()
                                            .id(reading.getId())
                                            .value(reading.getValue())
                                            .recordedOn(reading.getRecordedOn())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    private List<ReportResponse.ProductionProduct> buildProduction(Integer companyId) {
        return productRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(product -> {
                    List<ProductionRecord> records =
                            productionRecordRepository.findByProduct_IdOrderByRecordedOnAsc(product.getId());
                    double totalProduced = records.stream()
                            .mapToDouble(ProductionRecord::getQuantity)
                            .sum();

                    return ReportResponse.ProductionProduct.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .groupName(product.getGroup() != null ? product.getGroup().getName() : null)
                            .unit(product.getUnit())
                            .totalProduced(totalProduced)
                            .records(records.stream()
                                    .map(record -> ReportResponse.ProductionRecord.builder()
                                            .id(record.getId())
                                            .quantity(record.getQuantity())
                                            .quality(record.getQuality())
                                            .recordedOn(record.getRecordedOn())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }
}
