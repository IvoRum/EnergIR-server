package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateElectricMeterRequest;
import com.example.energier.server.model.request.UpdateElectricMeterRequest;
import com.example.energier.server.model.responce.ElectricMeterResponse;
import com.example.energier.server.persistence.ElectricMeter;
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

    public List<ElectricMeterResponse> getAll() {
        return electricMeterRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ElectricMeterResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public ElectricMeterResponse create(CreateElectricMeterRequest request) {
        var existing = electricMeterRepository.findByEdgeIdAndEdgeMeterId(request.getEdgeId(), request.getEdgeMeterId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        ElectricMeter meter = ElectricMeter.builder()
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

    public ElectricMeterResponse update(Integer id, UpdateElectricMeterRequest request) {
        ElectricMeter meter = findOrThrow(id);
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

    private ElectricMeter findOrThrow(Integer id) {
        return electricMeterRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meter with id " + id + " not found"));
    }

    private ElectricMeterResponse toResponse(ElectricMeter meter) {
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
                .build();
    }
}
