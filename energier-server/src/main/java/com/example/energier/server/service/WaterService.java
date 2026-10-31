package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateWaterGroupRequest;
import com.example.energier.server.model.request.CreateWaterMeterRequest;
import com.example.energier.server.model.request.CreateWaterReadingRequest;
import com.example.energier.server.model.request.UpdateWaterMeterRequest;
import com.example.energier.server.model.responce.WaterGroupResponse;
import com.example.energier.server.model.responce.WaterMeterResponse;
import com.example.energier.server.model.responce.WaterReadingResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.WaterGroup;
import com.example.energier.server.persistence.WaterMeter;
import com.example.energier.server.persistence.WaterReading;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.WaterGroupRepository;
import com.example.energier.server.repository.WaterMeterRepository;
import com.example.energier.server.repository.WaterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class WaterService {

    private final WaterMeterRepository meterRepository;
    private final WaterGroupRepository groupRepository;
    private final WaterReadingRepository readingRepository;
    private final CompanyRepository companyRepository;

    public List<WaterMeterResponse> getAll(Integer companyId) {
        return meterRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public WaterMeterResponse getById(Integer companyId, Integer id) {
        return toResponse(findMeter(companyId, id));
    }

    public WaterMeterResponse create(Integer companyId, CreateWaterMeterRequest request) {
        Company company = findCompany(companyId);

        WaterMeter meter = WaterMeter.builder()
                .company(company)
                .group(resolveGroup(companyId, request.getGroupId()))
                .name(requireText(request.getName(), "Meter name"))
                .description(trimToNull(request.getDescription()))
                .location(trimToNull(request.getLocation()))
                .unit(defaultIfBlank(request.getUnit(), "liters"))
                .status("active")
                .manufacturer(trimToNull(request.getManufacturer()))
                .installDate(parseDate(request.getInstallDate()))
                .currentValue(request.getCurrentValue() != null ? request.getCurrentValue() : 0)
                .createdOn(Instant.now())
                .build();

        return toResponse(meterRepository.save(meter));
    }

    public WaterMeterResponse update(Integer companyId, Integer id, UpdateWaterMeterRequest request) {
        WaterMeter meter = findMeter(companyId, id);
        boolean changed = false;

        if (request.getName() != null) {
            meter.setName(requireText(request.getName(), "Meter name"));
            changed = true;
        }
        if (request.getDescription() != null) {
            meter.setDescription(trimToNull(request.getDescription()));
            changed = true;
        }
        if (request.getLocation() != null) {
            meter.setLocation(trimToNull(request.getLocation()));
            changed = true;
        }
        if (request.getGroupId() != null) {
            meter.setGroup(resolveGroup(companyId, request.getGroupId()));
            changed = true;
        }
        if (request.getUnit() != null) {
            meter.setUnit(defaultIfBlank(request.getUnit(), "liters"));
            changed = true;
        }
        if (request.getStatus() != null) {
            meter.setStatus(defaultIfBlank(request.getStatus(), "active"));
            changed = true;
        }
        if (request.getManufacturer() != null) {
            meter.setManufacturer(trimToNull(request.getManufacturer()));
            changed = true;
        }
        if (request.getInstallDate() != null) {
            meter.setInstallDate(parseDate(request.getInstallDate()));
            changed = true;
        }
        if (request.getCurrentValue() != null) {
            meter.setCurrentValue(request.getCurrentValue());
            changed = true;
        }

        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        return toResponse(meterRepository.save(meter));
    }

    public void delete(Integer companyId, Integer id) {
        WaterMeter meter = findMeter(companyId, id);
        readingRepository.deleteAllByMeter_Id(meter.getId());
        meterRepository.delete(meter);
    }

    public List<WaterGroupResponse> getGroups(Integer companyId) {
        return groupRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toGroupResponse)
                .toList();
    }

    public WaterGroupResponse createGroup(Integer companyId, CreateWaterGroupRequest request) {
        Company company = findCompany(companyId);

        WaterGroup group = WaterGroup.builder()
                .company(company)
                .name(requireText(request.getName(), "Group name"))
                .description(trimToNull(request.getDescription()))
                .color(defaultIfBlank(request.getColor(), "#0ea5e9"))
                .createdOn(Instant.now())
                .build();

        return toGroupResponse(groupRepository.save(group));
    }

    public void deleteGroup(Integer companyId, Integer id) {
        WaterGroup group = groupRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Water group not found"));

        // Detach meters first so the group can be removed without deleting them.
        for (WaterMeter meter : meterRepository.findAllByCompany_IdOrderByIdAsc(companyId)) {
            if (meter.getGroup() != null && meter.getGroup().getId().equals(id)) {
                meter.setGroup(null);
                meterRepository.save(meter);
            }
        }

        groupRepository.delete(group);
    }

    public WaterMeterResponse addReading(Integer companyId, Integer meterId, CreateWaterReadingRequest request) {
        WaterMeter meter = findMeter(companyId, meterId);

        if (request.getValue() == null || request.getValue() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reading value must be greater than zero");
        }

        Instant recordedOn = parseInstant(request.getDate(), request.getTime());

        WaterReading reading = WaterReading.builder()
                .meter(meter)
                .value(request.getValue())
                .recordedOn(recordedOn)
                .createdOn(Instant.now())
                .build();
        readingRepository.save(reading);

        meter.setCurrentValue(meter.getCurrentValue() + request.getValue());
        meter.setLastUpdated(recordedOn);
        meter.setStatus("active");
        meterRepository.save(meter);

        return toResponse(meter);
    }

    public void deleteReading(Integer companyId, Integer meterId, Long readingId) {
        WaterMeter meter = findMeter(companyId, meterId);
        WaterReading reading = readingRepository.findByIdAndMeter_Id(readingId, meter.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Water reading not found"));

        readingRepository.delete(reading);
        meter.setCurrentValue(Math.max(0, meter.getCurrentValue() - reading.getValue()));
        meterRepository.save(meter);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Company findCompany(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private WaterMeter findMeter(Integer companyId, Integer id) {
        return meterRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Water meter with id " + id + " not found"));
    }

    private WaterGroup resolveGroup(Integer companyId, Integer groupId) {
        if (groupId == null || groupId <= 0) return null;
        return groupRepository.findByIdAndCompany_Id(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Water group not found"));
    }

    private Instant parseInstant(String date, String time) {
        if (date == null || date.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required");
        }
        try {
            LocalDate localDate = LocalDate.parse(date);
            LocalTime localTime = (time == null || time.isBlank())
                    ? LocalTime.now()
                    : LocalTime.parse(time);
            return localDate.atTime(localTime).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date or time");
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid install date");
        }
    }

    private String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String relativeTime(Instant instant) {
        if (instant == null) return "No readings";
        long minutes = ChronoUnit.MINUTES.between(instant, Instant.now());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        if (days < 30) return days + (days == 1 ? " day ago" : " days ago");
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    private WaterMeterResponse toResponse(WaterMeter meter) {
        List<WaterReading> readings =
                readingRepository.findByMeter_IdOrderByRecordedOnDesc(meter.getId());

        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();

        double monthlyTotal = readings.stream()
                .filter(r -> !r.getRecordedOn().isBefore(now.minus(30, ChronoUnit.DAYS)))
                .mapToDouble(WaterReading::getValue)
                .sum();

        List<Double> weeklyUsage = new ArrayList<>(Collections.nCopies(7, 0.0));
        LocalDate today = LocalDate.now(zone);
        for (WaterReading reading : readings) {
            LocalDate day = reading.getRecordedOn().atZone(zone).toLocalDate();
            long daysAgo = ChronoUnit.DAYS.between(day, today);
            if (daysAgo >= 0 && daysAgo < 7) {
                int index = 6 - (int) daysAgo;
                weeklyUsage.set(index, weeklyUsage.get(index) + reading.getValue());
            }
        }

        String installDate = meter.getInstallDate() != null
                ? meter.getInstallDate().toString()
                : meter.getCreatedOn().atZone(zone).toLocalDate().toString();

        return WaterMeterResponse.builder()
                .id(meter.getId())
                .name(meter.getName())
                .description(meter.getDescription())
                .location(meter.getLocation())
                .status(meter.getStatus())
                .groupId(meter.getGroup() != null ? meter.getGroup().getId() : null)
                .currentValue(meter.getCurrentValue())
                .unit(meter.getUnit())
                .lastUpdated(relativeTime(meter.getLastUpdated()))
                .weeklyUsage(weeklyUsage)
                .monthlyTotal(monthlyTotal)
                .dailyAverage(monthlyTotal / 30.0)
                .installDate(installDate)
                .manufacturer(meter.getManufacturer())
                .readings(readings.stream()
                        .map(reading -> toReadingResponse(reading, meter.getUnit()))
                        .toList())
                .build();
    }

    private WaterReadingResponse toReadingResponse(WaterReading reading, String meterUnit) {
        ZonedDateTime zoned = reading.getRecordedOn().atZone(ZoneId.systemDefault());
        return WaterReadingResponse.builder()
                .id(reading.getId())
                .meterId(reading.getMeter().getId())
                .value(reading.getValue())
                .unit(meterUnit)
                .date(zoned.toLocalDate().toString())
                .timestamp(reading.getRecordedOn().toEpochMilli())
                .build();
    }

    private WaterGroupResponse toGroupResponse(WaterGroup group) {
        return WaterGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .color(group.getColor())
                .build();
    }
}
