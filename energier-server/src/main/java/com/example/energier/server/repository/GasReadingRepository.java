package com.example.energier.server.repository;

import com.example.energier.server.persistence.GasReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface GasReadingRepository extends JpaRepository<GasReading, Long> {
    List<GasReading> findByMeter_IdOrderByRecordedOnDesc(Integer meterId);

    List<GasReading> findByMeter_IdAndRecordedOnGreaterThanEqualOrderByRecordedOnAsc(Integer meterId, Instant from);

    Optional<GasReading> findByIdAndMeter_Id(Long id, Integer meterId);

    void deleteAllByMeter_Id(Integer meterId);
}
