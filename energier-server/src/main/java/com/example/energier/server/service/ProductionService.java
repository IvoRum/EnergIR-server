package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateProductGroupRequest;
import com.example.energier.server.model.request.CreateProductRequest;
import com.example.energier.server.model.request.CreateProductionRecordRequest;
import com.example.energier.server.model.request.UpdateProductRequest;
import com.example.energier.server.model.responce.ProductGroupResponse;
import com.example.energier.server.model.responce.ProductResponse;
import com.example.energier.server.model.responce.ProductionRecordResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.Product;
import com.example.energier.server.persistence.ProductGroup;
import com.example.energier.server.persistence.ProductionRecord;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.ProductGroupRepository;
import com.example.energier.server.repository.ProductRepository;
import com.example.energier.server.repository.ProductionRecordRepository;
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
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductionService {

    private final ProductRepository productRepository;
    private final ProductGroupRepository productGroupRepository;
    private final ProductionRecordRepository productionRecordRepository;
    private final CompanyRepository companyRepository;

    public List<ProductResponse> getAll(Integer companyId) {
        return productRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getById(Integer companyId, Integer id) {
        return toResponse(findProduct(companyId, id));
    }

    public ProductResponse create(Integer companyId, CreateProductRequest request) {
        Company company = findCompany(companyId);

        Product product = Product.builder()
                .company(company)
                .group(resolveGroup(companyId, request.getGroupId()))
                .name(requireText(request.getName(), "Product name"))
                .description(trimToNull(request.getDescription()))
                .unit(defaultIfBlank(request.getUnit(), "pieces"))
                .createdOn(Instant.now())
                .build();

        return toResponse(productRepository.save(product));
    }

    public ProductResponse update(Integer companyId, Integer id, UpdateProductRequest request) {
        Product product = findProduct(companyId, id);
        boolean changed = false;

        if (request.getName() != null) {
            product.setName(requireText(request.getName(), "Product name"));
            changed = true;
        }
        if (request.getDescription() != null) {
            product.setDescription(trimToNull(request.getDescription()));
            changed = true;
        }
        if (request.getGroupId() != null) {
            product.setGroup(resolveGroup(companyId, request.getGroupId()));
            changed = true;
        }
        if (request.getUnit() != null) {
            product.setUnit(defaultIfBlank(request.getUnit(), "pieces"));
            changed = true;
        }

        if (!changed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }

        return toResponse(productRepository.save(product));
    }

    public void delete(Integer companyId, Integer id) {
        Product product = findProduct(companyId, id);
        productionRecordRepository.deleteAllByProduct_Id(product.getId());
        productRepository.delete(product);
    }

    public ProductResponse addRecord(Integer companyId, Integer productId, CreateProductionRecordRequest request) {
        Product product = findProduct(companyId, productId);

        Double quantity = request.getQuantity();
        if (quantity == null || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        ProductionRecord record = ProductionRecord.builder()
                .product(product)
                .quantity(quantity)
                .quality(trimToNull(request.getQuality()))
                .recordedOn(parseRecordedOn(request.getDate(), request.getTime()))
                .createdOn(Instant.now())
                .build();

        productionRecordRepository.save(record);
        return toResponse(product);
    }

    public void deleteRecord(Integer companyId, Integer productId, Long recordId) {
        Product product = findProduct(companyId, productId);
        ProductionRecord record = productionRecordRepository
                .findByIdAndProduct_Id(recordId, product.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Production record not found"));
        productionRecordRepository.delete(record);
    }

    public void deleteGroup(Integer companyId, Integer id) {
        ProductGroup group = productGroupRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product group not found"));

        // Detach products first so the group can be removed without touching products.
        for (Product product : productRepository.findAllByCompany_IdOrderByIdAsc(companyId)) {
            if (product.getGroup() != null && product.getGroup().getId().equals(id)) {
                product.setGroup(null);
                productRepository.save(product);
            }
        }

        productGroupRepository.delete(group);
    }

    public List<ProductGroupResponse> getGroups(Integer companyId) {
        return productGroupRepository.findAllByCompany_IdOrderByIdAsc(companyId).stream()
                .map(this::toGroupResponse)
                .toList();
    }

    public ProductGroupResponse createGroup(Integer companyId, CreateProductGroupRequest request) {
        Company company = findCompany(companyId);

        ProductGroup group = ProductGroup.builder()
                .company(company)
                .name(requireText(request.getName(), "Group name"))
                .description(trimToNull(request.getDescription()))
                .color(defaultIfBlank(request.getColor(), "#f97316"))
                .createdOn(Instant.now())
                .build();

        return toGroupResponse(productGroupRepository.save(group));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Company findCompany(Integer companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private Product findProduct(Integer companyId, Integer id) {
        return productRepository.findByIdAndCompany_Id(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product with id " + id + " not found"));
    }

    private ProductGroup resolveGroup(Integer companyId, Integer groupId) {
        if (groupId == null || groupId <= 0) return null;
        return productGroupRepository.findByIdAndCompany_Id(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product group not found"));
    }

    private Instant parseRecordedOn(String date, String time) {
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

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private ProductResponse toResponse(Product product) {
        List<ProductionRecord> records =
                productionRecordRepository.findByProduct_IdOrderByRecordedOnDesc(product.getId());

        double total = records.stream().mapToDouble(ProductionRecord::getQuantity).sum();

        Instant oldest = records.stream()
                .map(ProductionRecord::getRecordedOn)
                .min(Instant::compareTo)
                .orElse(product.getCreatedOn());
        long days = Math.max(1, ChronoUnit.DAYS.between(oldest, Instant.now()) + 1);
        double dailyAverage = total / days;

        Instant lastUpdated = records.isEmpty() ? product.getCreatedOn() : records.get(0).getRecordedOn();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .groupId(product.getGroup() != null ? product.getGroup().getId() : null)
                .unit(product.getUnit())
                .totalProduced(total)
                .dailyAverage(dailyAverage)
                .createdAt(product.getCreatedOn().atZone(ZoneId.systemDefault()).toLocalDate().toString())
                .lastUpdated(lastUpdated.toString())
                .productionData(records.stream().map(this::toRecordResponse).toList())
                .build();
    }

    private ProductionRecordResponse toRecordResponse(ProductionRecord record) {
        ZonedDateTime zoned = record.getRecordedOn().atZone(ZoneId.systemDefault());
        return ProductionRecordResponse.builder()
                .id(record.getId())
                .date(zoned.toLocalDate().toString())
                .time(zoned.toLocalTime().withNano(0).toString())
                .quantity(record.getQuantity())
                .quality(record.getQuality())
                .build();
    }

    private ProductGroupResponse toGroupResponse(ProductGroup group) {
        return ProductGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .color(group.getColor())
                .build();
    }
}
