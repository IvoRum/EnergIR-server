package com.example.energier.server.repository;

import com.example.energier.server.persistence.WaterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterReadingRepository extends JpaRepository<WaterReading, Long> {
    List<WaterReading> findByMeter_IdOrderByRecordedOnDesc(Integer meterId);

    List<WaterReading> findByMeter_IdOrderByRecordedOnAsc(Integer meterId);

    List<WaterReading> findByMeter_IdAndRecordedOnGreaterThanEqualOrderByRecordedOnAsc(Integer meterId, Instant from);

    Optional<WaterReading> findByIdAndMeter_Id(Long id, Integer meterId);

    void deleteAllByMeter_Id(Integer meterId);
}
