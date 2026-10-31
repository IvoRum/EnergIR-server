package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateDeviceRequest;
import com.example.energier.server.model.request.UpdateDeviceRequest;
import com.example.energier.server.model.responce.DeviceResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.Device;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class DeviceService {

    private static final Set<String> STATUSES = Set.of("online", "offline", "warning");

    private final DeviceRepository deviceRepository;
    private final CompanyRepository companyRepository;

    public List<DeviceResponse> getAll(Integer companyId) {
        return deviceRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public DeviceResponse getById(Integer companyId, Integer id) {
        return toResponse(findOrThrow(companyId, id));
    }

    /**
     * Edge devices call this periodically (or as part of a read cycle) to prove
     * they are alive. The returned status is derived from {@code lastSeen}.
     */
    public DeviceResponse heartbeat(Integer companyId, String code) {
        String normalized = code == null ? "" : code.trim();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device code is required");
        }

        Device device = deviceRepository.findByCompany_IdAndCode(companyId, normalized)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Device " + normalized + " not found"));

        device.setLastSeen(Instant.now());
        device.setStatus("online");
        return toResponse(deviceRepository.save(device));
    }

    public DeviceResponse create(Integer companyId, CreateDeviceRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        String code = requireText(request.getCode(), "Device code");
        if (deviceRepository.existsByCompany_IdAndCode(companyId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A device with this code already exists");
        }

        Device device = Device.builder()
                .company(company)
                .code(code)
                .name(requireText(request.getName(), "Device name"))
                .description(request.getDescription())
                .location(request.getLocation())
                .type(orDefault(request.getType(), "edge-v1"))
                .communicationType(orDefault(request.getCommunicationType(), "ModbusRTU"))
                .port(orDefault(request.getPort(), "COM3"))
                .baudrate(orDefault(request.getBaudrate(), "9600"))
                .parity(orDefault(request.getParity(), "N"))
                .dataBits(orDefault(request.getDataBits(), "8"))
                .status(normalizeStatus(request.getStatus()))
                .lastSeen(null)
                .createdOn(Instant.now())
                .build();

        return toResponse(deviceRepository.save(device));
    }

    public DeviceResponse update(Integer companyId, Integer id, UpdateDeviceRequest request) {
        Device device = findOrThrow(companyId, id);
        boolean changed = false;

        if (request.getCode() != null) {
            String code = requireText(request.getCode(), "Device code");
            if (!code.equals(device.getCode())) {
                if (deviceRepository.existsByCompany_IdAndCode(companyId, code)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A device with this code already exists");
                }
                device.setCode(code);
            }
            changed = true;
        }
        if (request.getName() != null) {
            device.setName(requireText(request.getName(), "Device name"));
            changed = true;
        }
        if (request.getDescription() != null) {
            device.setDescription(request.getDescription());
            changed = true;
        }
        if (request.getLocation() != null) {
            device.setLocation(request.getLocation());
            changed = true;
        }
        if (request.getType() != null) {
            device.setType(request.getType());
            changed = true;
        }
        if (request.getCommunicationType() != null) {
            device.setCommunicationType(request.getCommunicationType());
            changed = true;
        }
        if (request.getPort() != null) {
            device.setPort(request.getPort());
            changed = true;
        }
        if (request.getBaudrate() != null) {
            device.setBaudrate(request.getBaudrate());
            changed = true;
        }
        if (request.getParity() != null) {
            device.setParity(request.getParity());
            changed = true;
        }
        if (request.getDataBits() != null) {
            device.setDataBits(request.getDataBits());
            changed = true;
        }
        if (request.getStatus() != null) {
            device.setStatus(normalizeStatus(request.getStatus()));
            changed = true;
        }
        if (request.getLastSeen() != null) {
            device.setLastSeen(request.getLastSeen());
            changed = true;
        }

        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        return toResponse(deviceRepository.save(device));
    }

    private Device findOrThrow(Integer companyId, Integer id) {
        return deviceRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Device with id " + id + " not found"));
    }

    private String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return trimmed;
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        return STATUSES.contains(normalized) ? normalized : "offline";
    }

    private DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .code(device.getCode())
                .name(device.getName())
                .description(device.getDescription())
                .location(device.getLocation())
                .type(device.getType())
                .communicationType(device.getCommunicationType())
                .port(device.getPort())
                .baudrate(device.getBaudrate())
                .parity(device.getParity())
                .dataBits(device.getDataBits())
                .status(effectiveStatus(device))
                .lastSeen(device.getLastSeen())
                .createdOn(device.getCreatedOn())
                .build();
    }

    /**
     * Online if the device was seen in the last 5 minutes, warning up to 15
     * minutes, otherwise offline. A device that has never checked in is offline.
     */
    private String effectiveStatus(Device device) {
        Instant lastSeen = device.getLastSeen();
        if (lastSeen == null) return "offline";

        long ageSeconds = Duration.between(lastSeen, Instant.now()).getSeconds();
        if (ageSeconds <= 5 * 60) return "online";
        if (ageSeconds <= 15 * 60) return "warning";
        return "offline";
    }
}
