package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateCompressorReadingRequest;
import com.example.energier.server.model.request.CreateCompressorRequest;
import com.example.energier.server.model.request.UpdateCompressorRequest;
import com.example.energier.server.model.responce.CompressorReadingResponse;
import com.example.energier.server.model.responce.CompressorResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.Compressor;
import com.example.energier.server.persistence.CompressorReading;
import com.example.energier.server.persistence.ElectricMeter;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.CompressorReadingRepository;
import com.example.energier.server.repository.CompressorRepository;
import com.example.energier.server.repository.ElectricMeterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Compressors and their air output, in cubic metres. Company-scoped. */
@Service
@Transactional
@RequiredArgsConstructor
public class CompressorService {

    private final CompressorRepository compressorRepository;
    private final CompressorReadingRepository readingRepository;
    private final CompanyRepository companyRepository;
    private final ElectricMeterRepository electricMeterRepository;

    public List<CompressorResponse> getAll(Integer companyId) {
        return getAll(companyId, false);
    }

    /** archived=true lists the archived compressors instead of the active ones. */
    public List<CompressorResponse> getAll(Integer companyId, boolean archived) {
        return compressorRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .filter(unit -> Boolean.TRUE.equals(unit.getArchived()) == archived)
                .map(this::toResponse)
                .toList();
    }

    public CompressorResponse getById(Integer companyId, Integer id) {
        return toResponse(findCompressor(companyId, id));
    }

    public CompressorResponse create(Integer companyId, CreateCompressorRequest request) {
        Company company = findCompany(companyId);

        Compressor compressor = Compressor.builder()
                .company(company)
                .name(requireText(request.getName(), "Compressor name"))
                .description(trimToNull(request.getDescription()))
                .electricMeter(resolveMeter(companyId, request.getElectricMeterId()))
                .createdOn(Instant.now())
                .build();

        return toResponse(compressorRepository.save(compressor));
    }

    public CompressorResponse update(Integer companyId, Integer id, UpdateCompressorRequest request) {
        Compressor compressor = findCompressor(companyId, id);

        if (request.getName() != null) compressor.setName(requireText(request.getName(), "Compressor name"));
        if (request.getDescription() != null) compressor.setDescription(trimToNull(request.getDescription()));
        // null = keep, 0 = detach, >0 = attach that meter.
        if (request.getElectricMeterId() != null) {
            compressor.setElectricMeter(resolveMeter(companyId, request.getElectricMeterId()));
        }

        return toResponse(compressorRepository.save(compressor));
    }

    /** Soft delete / restore. Archiving keeps the readings intact. */
    public CompressorResponse setArchived(Integer companyId, Integer id, boolean archived) {
        Compressor compressor = findCompressor(companyId, id);
        compressor.setArchived(archived);
        return toResponse(compressorRepository.save(compressor));
    }

    /** Hard delete: removes the compressor and all of its readings. */
    public void delete(Integer companyId, Integer id) {
        Compressor compressor = findCompressor(companyId, id);
        readingRepository.deleteAllByCompressor_Id(compressor.getId());
        compressorRepository.delete(compressor);
    }

    public CompressorResponse addReading(Integer companyId, Integer id, CreateCompressorReadingRequest request) {
        Compressor compressor = findCompressor(companyId, id);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        Instant recordedOn = parseInstant(request.getDate(), request.getTime());

        readingRepository.save(CompressorReading.builder()
                .compressor(compressor)
                .quantity(request.getQuantity())
                .recordedOn(recordedOn)
                .createdOn(Instant.now())
                .build());

        return toResponse(compressor);
    }

    /** Edits one reading (quantity and/or timestamp). */
    public CompressorResponse updateReading(
            Integer companyId,
            Integer id,
            Long readingId,
            CreateCompressorReadingRequest request
    ) {
        Compressor compressor = findCompressor(companyId, id);
        CompressorReading reading = readingRepository.findByIdAndCompressor_Id(readingId, compressor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compressor reading not found"));

        if (request.getQuantity() != null) {
            if (request.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
            }
            reading.setQuantity(request.getQuantity());
        }
        if (request.getDate() != null || request.getTime() != null) {
            var previous = reading.getRecordedOn().atZone(ZoneId.systemDefault());
            String date = (request.getDate() == null || request.getDate().isBlank())
                    ? previous.toLocalDate().toString()
                    : request.getDate();
            String time = (request.getTime() == null || request.getTime().isBlank())
                    ? previous.toLocalTime().toString()
                    : request.getTime();
            reading.setRecordedOn(parseInstant(date, time));
        }
        readingRepository.save(reading);

        return toResponse(compressor);
    }

    public void deleteReading(Integer companyId, Integer id, Long readingId) {
        Compressor compressor = findCompressor(companyId, id);
        CompressorReading reading = readingRepository.findByIdAndCompressor_Id(readingId, compressor.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compressor reading not found"));
        readingRepository.delete(reading);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Company findCompany(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private Compressor findCompressor(Integer companyId, Integer id) {
        return compressorRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compressor with id " + id + " not found"));
    }

    /** null/0 detaches the meter; anything else must belong to the same company. */
    private ElectricMeter resolveMeter(Integer companyId, Integer meterId) {
        if (meterId == null || meterId <= 0) return null;
        return electricMeterRepository.findByIdAndCompany_Id(meterId, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Electric meter with id " + meterId + " not found"));
    }

    private Instant parseInstant(String date, String time) {
        if (date == null || date.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required");
        }
        try {
            LocalDate localDate = LocalDate.parse(date);
            LocalTime localTime = (time == null || time.isBlank())
                    ? LocalTime.MIDNIGHT
                    : LocalTime.parse(time);
            return localDate.atTime(localTime).atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date or time");
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

    private CompressorResponse toResponse(Compressor compressor) {
        List<CompressorReading> readings =
                readingRepository.findByCompressor_IdOrderByRecordedOnDesc(compressor.getId());

        double total = readings.stream().mapToDouble(CompressorReading::getQuantity).sum();

        Instant oldest = readings.stream()
                .map(CompressorReading::getRecordedOn)
                .min(Instant::compareTo)
                .orElse(compressor.getCreatedOn());
        long days = Math.max(1, ChronoUnit.DAYS.between(oldest, Instant.now()) + 1);

        Instant lastUpdated = readings.isEmpty() ? compressor.getCreatedOn() : readings.get(0).getRecordedOn();

        return CompressorResponse.builder()
                .id(compressor.getId())
                .archived(Boolean.TRUE.equals(compressor.getArchived()))
                .name(compressor.getName())
                .description(compressor.getDescription())
                .electricMeterId(compressor.getElectricMeter() != null ? compressor.getElectricMeter().getId() : null)
                .electricMeterName(compressor.getElectricMeter() != null ? compressor.getElectricMeter().getName() : null)
                .totalProduced(total)
                .dailyAverage(total / days)
                .createdAt(compressor.getCreatedOn().atZone(ZoneId.systemDefault()).toLocalDate().toString())
                .lastUpdated(lastUpdated.atZone(ZoneId.systemDefault()).toLocalDateTime().toString())
                .readings(readings.stream().map(this::toReadingResponse).toList())
                .build();
    }

    private CompressorReadingResponse toReadingResponse(CompressorReading reading) {
        var local = reading.getRecordedOn().atZone(ZoneId.systemDefault());
        return CompressorReadingResponse.builder()
                .id(reading.getId())
                .quantity(reading.getQuantity())
                .unit("m3")
                .date(local.toLocalDate().toString())
                .time(local.toLocalTime().toString())
                .build();
    }
}
