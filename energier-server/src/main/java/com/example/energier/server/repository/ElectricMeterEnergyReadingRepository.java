package com.example.energier.server.repository;

import com.example.energier.server.persistence.ElectricMeterEnergyReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ElectricMeterEnergyReadingRepository extends JpaRepository<ElectricMeterEnergyReading, Long> {
    Optional<ElectricMeterEnergyReading> findByMeter_IdAndCreatedOn(Integer meterId, Instant createdOn);

    Optional<ElectricMeterEnergyReading> findFirstByMeter_IdOrderByCreatedOnDesc(Integer meterId);

    List<ElectricMeterEnergyReading> findByMeter_IdAndCreatedOnGreaterThanEqualOrderByIdAsc(
            Integer meterId, Instant since, Pageable pageable);
}
