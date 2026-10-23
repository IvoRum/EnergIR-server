package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateElectricMeterEnergyReadingRequest;
import com.example.energier.server.model.request.CreateMeterReadingRequest;
import com.example.energier.server.model.responce.ElectricMeterEnergyReadingResponse;
import com.example.energier.server.persistence.ElectricMeter;
import com.example.energier.server.persistence.ElectricMeterEnergyReading;
import com.example.energier.server.repository.ElectricMeterEnergyReadingRepository;
import com.example.energier.server.repository.DeviceRepository;
import com.example.energier.server.repository.ElectricMeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ElectricMeterEnergyReadingService {

    private final ElectricMeterEnergyReadingRepository readingRepository;
    private final ElectricMeterRepository meterRepository;
    private final DeviceRepository deviceRepository;

    public ElectricMeterEnergyReadingResponse create(CreateElectricMeterEnergyReadingRequest request) {        ElectricMeter meter = meterRepository.findByEdgeIdAndEdgeMeterId(request.getEdgeId(), request.getEdgeMeterId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No meter registered for edgeId=" + request.getEdgeId() + ", edgeMeterId=" + request.getEdgeMeterId()));

        Instant createdOn = request.getCreatedOn() != null ? request.getCreatedOn() : Instant.now();

        // Uploading a reading proves the device is alive.
        touchDevice(meter);

        var existing = readingRepository.findByMeter_IdAndCreatedOn(meter.getId(), createdOn);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        ElectricMeterEnergyReading reading = ElectricMeterEnergyReading.builder()
                .meter(meter)
                .totalActiveEnergyImportTariff1(request.getTotalActiveEnergyImportTariff1())
                .totalActiveEnergyImportTariff2(request.getTotalActiveEnergyImportTariff2())
                .totalActiveEnergyExportTariff1(request.getTotalActiveEnergyExportTariff1())
                .totalActiveEnergyExportTariff2(request.getTotalActiveEnergyExportTariff2())
                .totalReactiveEnergyImportTariff1(request.getTotalReactiveEnergyImportTariff1())
                .totalReactiveEnergyImportTariff2(request.getTotalReactiveEnergyImportTariff2())
                .totalReactiveEnergyExportTariff1(request.getTotalReactiveEnergyExportTariff1())
                .totalReactiveEnergyExportTariff2(request.getTotalReactiveEnergyExportTariff2())
                .totalApparentEnergyTariff1(request.getTotalApparentEnergyTariff1())
                .totalApparentEnergyTariff2(request.getTotalApparentEnergyTariff2())
                .createdOn(createdOn)
                .build();

        return toResponse(readingRepository.save(reading));
    }

    /** Creates a reading entered manually from the dashboard (single kWh value). */
    public ElectricMeterEnergyReadingResponse createForMeter(
            Integer companyId,
            Integer meterId,
            CreateMeterReadingRequest request
    ) {
        ElectricMeter meter = meterRepository.findByIdAndCompany_Id(meterId, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Meter with id " + meterId + " not found"));

        if (request.getValue() == null || request.getValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reading value must be greater than zero");
        }

        double kwh = toKwh(request.getValue(), request.getUnit());
        Instant createdOn = request.getCreatedOn() != null ? request.getCreatedOn() : Instant.now();

        touchDevice(meter);

        var existing = readingRepository.findByMeter_IdAndCreatedOn(meter.getId(), createdOn);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Manual entries are "usage since the last reading", so add them to the
        // latest cumulative total instead of replacing it.
        ElectricMeterEnergyReading latest = readingRepository
                .findFirstByMeter_IdOrderByCreatedOnDesc(meter.getId())
                .orElse(null);

        // A first manual entry has no baseline; seed zero so its usage is not lost.
        if (latest == null) {
            ElectricMeterEnergyReading baseline = ElectricMeterEnergyReading.builder()
                    .meter(meter)
                    .totalActiveEnergyImportTariff1(0)
                    .totalActiveEnergyImportTariff2(0)
                    .totalActiveEnergyExportTariff1(0)
                    .totalActiveEnergyExportTariff2(0)
                    .totalReactiveEnergyImportTariff1(0)
                    .totalReactiveEnergyImportTariff2(0)
                    .totalReactiveEnergyExportTariff1(0)
                    .totalReactiveEnergyExportTariff2(0)
                    .totalApparentEnergyTariff1(0)
                    .totalApparentEnergyTariff2(0)
                    .createdOn(createdOn.minusSeconds(1))
                    .build();
            readingRepository.save(baseline);
        }

        double base = latest != null
                ? latest.getTotalActiveEnergyImportTariff1() + latest.getTotalActiveEnergyImportTariff2()
                : 0.0;
        double newTotal = base + kwh;

        ElectricMeterEnergyReading reading = ElectricMeterEnergyReading.builder()
                .meter(meter)
                .totalActiveEnergyImportTariff1(newTotal)
                .totalActiveEnergyImportTariff2(0)
                .totalActiveEnergyExportTariff1(0)
                .totalActiveEnergyExportTariff2(0)
                .totalReactiveEnergyImportTariff1(0)
                .totalReactiveEnergyImportTariff2(0)
                .totalReactiveEnergyExportTariff1(0)
                .totalReactiveEnergyExportTariff2(0)
                .totalApparentEnergyTariff1(0)
                .totalApparentEnergyTariff2(0)
                .createdOn(createdOn)
                .build();

        return toResponse(readingRepository.save(reading));
    }

    private double toKwh(double value, String unit) {
        if (unit == null) return value;
        return switch (unit.trim().toLowerCase()) {
            case "mwh" -> value * 1000.0;
            case "wh" -> value / 1000.0;
            default -> value;
        };
    }

    /** Marks the device that owns this meter as seen just now. */
    private void touchDevice(ElectricMeter meter) {
        deviceRepository.findByCompany_IdAndCode(meter.getCompany().getId(), meter.getEdgeId())
                .ifPresent(device -> {
                    device.setLastSeen(Instant.now());
                    device.setStatus("online");
                    deviceRepository.save(device);
                });
    }

    public List<ElectricMeterEnergyReadingResponse> getHistory(Integer meterId, Instant since, int limit) {        if (!meterRepository.existsById(meterId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter with id " + meterId + " not found");
        }

        Instant from = since != null ? since : Instant.EPOCH;
        return readingRepository
                .findByMeter_IdAndCreatedOnGreaterThanEqualOrderByIdAsc(meterId, from, PageRequest.of(0, limit))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ElectricMeterEnergyReadingResponse toResponse(ElectricMeterEnergyReading reading) {
        return ElectricMeterEnergyReadingResponse.builder()
                .id(reading.getId())
                .meterId(reading.getMeter().getId())
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
                .createdOn(reading.getCreatedOn())
                .build();
    }
}
