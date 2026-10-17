package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateElectricMeterRequest;
import com.example.energier.server.model.request.UpdateElectricMeterRequest;
import com.example.energier.server.model.responce.ElectricMeterEnergyReadingResponse;
import com.example.energier.server.model.responce.ElectricMeterResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.ElectricMeter;
import com.example.energier.server.persistence.ElectricMeterEnergyReading;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.ElectricMeterEnergyReadingRepository;
import com.example.energier.server.repository.ElectricMeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ElectricMeterService {

    private final ElectricMeterRepository electricMeterRepository;
    private final ElectricMeterEnergyReadingRepository readingRepository;
    private final CompanyRepository companyRepository;

    /** All meters belonging to a company. */
    public List<ElectricMeterResponse> getAll(Integer companyId) {
        return electricMeterRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ElectricMeterResponse getById(Integer companyId, Integer id) {
        return toResponse(findOrThrow(companyId, id));
    }

    /** Creates a meter under the caller's company (idempotent per edge meter). */
    public ElectricMeterResponse create(Integer companyId, CreateElectricMeterRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        var existing = electricMeterRepository
                .findByCompany_IdAndEdgeIdAndEdgeMeterId(companyId, request.getEdgeId(), request.getEdgeMeterId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        ElectricMeter meter = ElectricMeter.builder()
                .company(company)
                .edgeId(request.getEdgeId())
                .edgeMeterId(request.getEdgeMeterId())
                .address(request.getAddress())
                .name(request.getName())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .active(true)
                .createdOn(Instant.now())
                .build();

        return toResponse(electricMeterRepository.save(meter));
    }

    public ElectricMeterResponse update(Integer companyId, Integer id, UpdateElectricMeterRequest request) {
        ElectricMeter meter = findOrThrow(companyId, id);
        boolean changed = false;

        if (request.getName() != null) {
            meter.setName(request.getName());
            changed = true;
        }
        if (request.getAddress() != null) {
            meter.setAddress(request.getAddress());
            changed = true;
        }
        if (request.getManufacturer() != null) {
            meter.setManufacturer(request.getManufacturer());
            changed = true;
        }
        if (request.getModel() != null) {
            meter.setModel(request.getModel());
            changed = true;
        }
        if (request.getActive() != null) {
            meter.setActive(request.getActive());
            changed = true;
        }

        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        return toResponse(electricMeterRepository.save(meter));
    }

    private ElectricMeter findOrThrow(Integer companyId, Integer id) {
        return electricMeterRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Meter with id " + id + " not found"));
    }

    private ElectricMeterResponse toResponse(ElectricMeter meter) {
        ElectricMeterEnergyReading latest = readingRepository
                .findFirstByMeter_IdOrderByCreatedOnDesc(meter.getId())
                .orElse(null);

        return ElectricMeterResponse.builder()
                .id(meter.getId())
                .edgeId(meter.getEdgeId())
                .edgeMeterId(meter.getEdgeMeterId())
                .address(meter.getAddress())
                .name(meter.getName())
                .manufacturer(meter.getManufacturer())
                .model(meter.getModel())
                .active(meter.isActive())
                .createdOn(meter.getCreatedOn())
                .latestReading(latest != null ? toReadingResponse(latest) : null)
                .build();
    }

    private ElectricMeterEnergyReadingResponse toReadingResponse(ElectricMeterEnergyReading reading) {
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
