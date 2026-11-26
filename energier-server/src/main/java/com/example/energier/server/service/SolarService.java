package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateSolarGroupRequest;
import com.example.energier.server.model.request.CreateSolarPanelRequest;
import com.example.energier.server.model.request.CreateSolarReadingRequest;
import com.example.energier.server.model.request.UpdateSolarGroupRequest;
import com.example.energier.server.model.request.UpdateSolarPanelRequest;
import com.example.energier.server.model.request.UpdateSolarReadingRequest;
import com.example.energier.server.model.responce.SolarGroupResponse;
import com.example.energier.server.model.responce.SolarPanelResponse;
import com.example.energier.server.model.responce.SolarReadingResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.SolarGroup;
import com.example.energier.server.persistence.SolarPanel;
import com.example.energier.server.persistence.SolarReading;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.SolarGroupRepository;
import com.example.energier.server.repository.SolarPanelRepository;
import com.example.energier.server.repository.SolarReadingRepository;
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
public class SolarService {

    private final SolarPanelRepository panelRepository;
    private final SolarGroupRepository groupRepository;
    private final SolarReadingRepository readingRepository;
    private final CompanyRepository companyRepository;

    public List<SolarPanelResponse> getAll(Integer companyId) {
        return getAll(companyId, false);
    }

    /** archived=true lists the archived panels instead of the active ones. */
    public List<SolarPanelResponse> getAll(Integer companyId, boolean archived) {
        return panelRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .filter(panel -> Boolean.TRUE.equals(panel.getArchived()) == archived)
                .map(this::toResponse)
                .toList();
    }

    /** Soft delete / restore. Archiving keeps the panel readings intact. */
    public SolarPanelResponse setArchived(Integer companyId, Integer id, boolean archived) {
        SolarPanel panel = findPanel(companyId, id);
        panel.setArchived(archived);
        return toResponse(panelRepository.save(panel));
    }

    public SolarPanelResponse getById(Integer companyId, Integer id) {
        return toResponse(findPanel(companyId, id));
    }

    public SolarPanelResponse create(Integer companyId, CreateSolarPanelRequest request) {
        Company company = findCompany(companyId);

        double capacity = positiveOrZero(request.getCapacity(), "Capacity");
        double currentOutput = positiveOrZero(request.getCurrentOutput(), "Current output");

        SolarPanel panel = SolarPanel.builder()
                .company(company)
                .group(resolveGroup(companyId, request.getGroupId()))
                .name(requireText(request.getName(), "Panel name"))
                .description(trimToNull(request.getDescription()))
                .location(trimToNull(request.getLocation()))
                .unit(defaultIfBlank(request.getUnit(), "kW"))
                .status(defaultIfBlank(request.getStatus(), statusFor(currentOutput, capacity)))
                .manufacturer(trimToNull(request.getManufacturer()))
                .installDate(parseDate(request.getInstallDate()))
                .capacity(capacity)
                .efficiency(clampPercentage(request.getEfficiency(), currentOutput, capacity))
                .currentOutput(currentOutput)
                .createdOn(Instant.now())
                .build();

        return toResponse(panelRepository.save(panel));
    }

    public SolarPanelResponse update(Integer companyId, Integer id, UpdateSolarPanelRequest request) {
        SolarPanel panel = findPanel(companyId, id);
        boolean changed = false;

        if (request.getName() != null) {
            panel.setName(requireText(request.getName(), "Panel name"));
            changed = true;
        }
        if (request.getDescription() != null) {
            panel.setDescription(trimToNull(request.getDescription()));
            changed = true;
        }
        if (request.getLocation() != null) {
            panel.setLocation(trimToNull(request.getLocation()));
            changed = true;
        }
        if (request.getGroupId() != null) {
            panel.setGroup(resolveGroup(companyId, request.getGroupId()));
            changed = true;
        }
        if (request.getUnit() != null) {
            panel.setUnit(defaultIfBlank(request.getUnit(), "kW"));
            changed = true;
        }
        if (request.getStatus() != null) {
            panel.setStatus(defaultIfBlank(request.getStatus(), "generating"));
            changed = true;
        }
        if (request.getManufacturer() != null) {
            panel.setManufacturer(trimToNull(request.getManufacturer()));
            changed = true;
        }
        if (request.getInstallDate() != null) {
            panel.setInstallDate(parseDate(request.getInstallDate()));
            changed = true;
        }
        if (request.getCapacity() != null) {
            panel.setCapacity(positiveOrZero(request.getCapacity(), "Capacity"));
            changed = true;
        }
        if (request.getCurrentOutput() != null) {
            panel.setCurrentOutput(positiveOrZero(request.getCurrentOutput(), "Current output"));
            changed = true;
        }
        if (request.getEfficiency() != null) {
            panel.setEfficiency(positiveOrZero(request.getEfficiency(), "Efficiency"));
            changed = true;
        }

        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        // Keep efficiency and status aligned with the rated capacity.
        panel.setEfficiency(clampPercentage(panel.getEfficiency(), panel.getCurrentOutput(), panel.getCapacity()));
        if (request.getStatus() == null) {
            panel.setStatus(statusFor(panel.getCurrentOutput(), panel.getCapacity()));
        }

        return toResponse(panelRepository.save(panel));
    }

    public void delete(Integer companyId, Integer id) {
        SolarPanel panel = findPanel(companyId, id);
        readingRepository.deleteAllByPanel_Id(panel.getId());
        panelRepository.delete(panel);
    }

    public List<SolarGroupResponse> getGroups(Integer companyId) {
        return groupRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toGroupResponse)
                .toList();
    }

    public SolarGroupResponse createGroup(Integer companyId, CreateSolarGroupRequest request) {
        Company company = findCompany(companyId);

        SolarGroup group = SolarGroup.builder()
                .company(company)
                .name(requireText(request.getName(), "Group name"))
                .description(trimToNull(request.getDescription()))
                .color(defaultIfBlank(request.getColor(), "#f59e0b"))
                .createdOn(Instant.now())
                .build();

        return toGroupResponse(groupRepository.save(group));
    }

    public void deleteGroup(Integer companyId, Integer id) {
        SolarGroup group = groupRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solar group not found"));

        // Detach panels first so the group can be removed without deleting them.
        for (SolarPanel panel : panelRepository.findAllByCompany_IdOrderByIdAsc(companyId)) {
            if (panel.getGroup() != null && panel.getGroup().getId().equals(id)) {
                panel.setGroup(null);
                panelRepository.save(panel);
            }
        }

        groupRepository.delete(group);
    }

    public SolarPanelResponse addReading(Integer companyId, Integer panelId, CreateSolarReadingRequest request) {
        SolarPanel panel = findPanel(companyId, panelId);

        if (request.getPowerOutput() == null || request.getPowerOutput() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Power output cannot be negative");
        }

        Instant recordedOn = parseInstant(request.getDate(), request.getTime());

        SolarReading reading = SolarReading.builder()
                .panel(panel)
                .powerOutput(request.getPowerOutput())
                .irradiance(positiveOrNull(request.getIrradiance(), "Irradiance"))
                .recordedOn(recordedOn)
                .createdOn(Instant.now())
                .build();
        readingRepository.save(reading);

        refreshFromLatestReading(panel);

        return toResponse(panel);
    }

    public void deleteReading(Integer companyId, Integer panelId, Long readingId) {
        SolarPanel panel = findPanel(companyId, panelId);
        SolarReading reading = readingRepository.findByIdAndPanel_Id(readingId, panel.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solar reading not found"));

        readingRepository.delete(reading);
        refreshFromLatestReading(panel);
    }

    /** Edits a stored reading and keeps the panel's latest output in step. */
    public SolarPanelResponse updateReading(
            Integer companyId,
            Integer panelId,
            Long readingId,
            UpdateSolarReadingRequest request
    ) {
        SolarPanel panel = findPanel(companyId, panelId);
        SolarReading reading = readingRepository.findByIdAndPanel_Id(readingId, panel.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solar reading not found"));

        if (request.getPowerOutput() != null) {
            if (request.getPowerOutput() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Power output cannot be negative");
            }
            reading.setPowerOutput(request.getPowerOutput());
        }
        if (request.getIrradiance() != null) {
            reading.setIrradiance(positiveOrNull(request.getIrradiance(), "Irradiance"));
        }
        if (request.getDate() != null || request.getTime() != null) {
            String date = (request.getDate() == null || request.getDate().isBlank())
                    ? reading.getRecordedOn().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                    : request.getDate();
            reading.setRecordedOn(parseInstant(date, request.getTime()));
        }
        readingRepository.save(reading);

        refreshFromLatestReading(panel);

        return toResponse(panel);
    }

    public SolarGroupResponse updateGroup(Integer companyId, Integer id, UpdateSolarGroupRequest request) {
        SolarGroup group = groupRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solar group not found"));

        if (request.getName() != null) group.setName(requireText(request.getName(), "Group name"));
        if (request.getDescription() != null) group.setDescription(trimToNull(request.getDescription()));
        if (request.getColor() != null) group.setColor(defaultIfBlank(request.getColor(), group.getColor()));

        return toGroupResponse(groupRepository.save(group));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Current output is the newest reading, so deleting/editing one must recompute it. */
    private void refreshFromLatestReading(SolarPanel panel) {
        List<SolarReading> readings = readingRepository.findByPanel_IdOrderByRecordedOnDesc(panel.getId());
        if (readings.isEmpty()) {
            panel.setCurrentOutput(0);
            panel.setEfficiency(0);
            panel.setLastUpdated(null);
            panel.setStatus("offline");
        } else {
            SolarReading latest = readings.get(0);
            panel.setCurrentOutput(latest.getPowerOutput());
            panel.setLastUpdated(latest.getRecordedOn());
            panel.setEfficiency(clampPercentage(null, latest.getPowerOutput(), panel.getCapacity()));
            panel.setStatus(statusFor(latest.getPowerOutput(), panel.getCapacity()));
        }
        panelRepository.save(panel);
    }

    private String statusFor(double currentOutput, double capacity) {
        if (currentOutput <= 0) return "offline";
        if (capacity > 0 && currentOutput < capacity * 0.5) return "low_output";
        return "generating";
    }

    private double positiveOrZero(Double value, String field) {
        if (value == null) return 0;
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " cannot be negative");
        }
        return value;
    }

    private Double positiveOrNull(Double value, String field) {
        if (value == null) return null;
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " cannot be negative");
        }
        return value;
    }

    private double clampPercentage(Double stored, double currentOutput, double capacity) {
        if (capacity <= 0) return stored != null ? Math.min(100, Math.max(0, stored)) : 0;
        double computed = (currentOutput / capacity) * 100.0;
        return Math.min(100, Math.max(0, computed));
    }

    private Company findCompany(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private SolarPanel findPanel(Integer companyId, Integer id) {
        return panelRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solar panel with id " + id + " not found"));
    }

    private SolarGroup resolveGroup(Integer companyId, Integer groupId) {
        if (groupId == null || groupId <= 0) return null;
        return groupRepository.findByIdAndCompany_Id(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solar group not found"));
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

    private SolarPanelResponse toResponse(SolarPanel panel) {
        List<SolarReading> readings =
                readingRepository.findByPanel_IdOrderByRecordedOnDesc(panel.getId());

        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();

        double monthlyTotal = readings.stream()
                .filter(r -> !r.getRecordedOn().isBefore(now.minus(30, ChronoUnit.DAYS)))
                .mapToDouble(SolarReading::getPowerOutput)
                .sum();

        List<Double> weeklyGeneration = new ArrayList<>(Collections.nCopies(7, 0.0));
        LocalDate today = LocalDate.now(zone);
        for (SolarReading reading : readings) {
            LocalDate day = reading.getRecordedOn().atZone(zone).toLocalDate();
            long daysAgo = ChronoUnit.DAYS.between(day, today);
            if (daysAgo >= 0 && daysAgo < 7) {
                int index = 6 - (int) daysAgo;
                weeklyGeneration.set(index, weeklyGeneration.get(index) + reading.getPowerOutput());
            }
        }

        String installDate = panel.getInstallDate() != null
                ? panel.getInstallDate().toString()
                : panel.getCreatedOn().atZone(zone).toLocalDate().toString();

        return SolarPanelResponse.builder()
                .id(panel.getId())
                .archived(Boolean.TRUE.equals(panel.getArchived()))
                .name(panel.getName())
                .description(panel.getDescription())
                .location(panel.getLocation())
                .status(panel.getStatus())
                .groupId(panel.getGroup() != null ? panel.getGroup().getId() : null)
                .currentOutput(panel.getCurrentOutput())
                .unit(panel.getUnit())
                .lastUpdated(relativeTime(panel.getLastUpdated()))
                .weeklyGeneration(weeklyGeneration)
                .monthlyTotal(monthlyTotal)
                .dailyAverage(monthlyTotal / 30.0)
                .capacity(panel.getCapacity())
                .efficiency(panel.getEfficiency())
                .installDate(installDate)
                .manufacturer(panel.getManufacturer())
                .readings(readings.stream()
                        .map(reading -> toReadingResponse(reading, panel.getUnit()))
                        .toList())
                .build();
    }

    private SolarReadingResponse toReadingResponse(SolarReading reading, String panelUnit) {
        ZonedDateTime zoned = reading.getRecordedOn().atZone(ZoneId.systemDefault());
        return SolarReadingResponse.builder()
                .id(reading.getId())
                .panelId(reading.getPanel().getId())
                .powerOutput(reading.getPowerOutput())
                .unit(panelUnit)
                .irradiance(reading.getIrradiance())
                .date(zoned.toLocalDate().toString())
                .timestamp(reading.getRecordedOn().toEpochMilli())
                .build();
    }

    private SolarGroupResponse toGroupResponse(SolarGroup group) {
        return SolarGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .color(group.getColor())
                .build();
    }
}
