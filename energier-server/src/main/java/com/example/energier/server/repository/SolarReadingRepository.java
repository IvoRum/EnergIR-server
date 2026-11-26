package com.example.energier.server.repository;

import com.example.energier.server.persistence.SolarReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolarReadingRepository extends JpaRepository<SolarReading, Long> {
    List<SolarReading> findByPanel_IdOrderByRecordedOnDesc(Integer panelId);

    List<SolarReading> findByPanel_IdOrderByRecordedOnAsc(Integer panelId);

    List<SolarReading> findByPanel_IdAndRecordedOnGreaterThanEqualOrderByRecordedOnAsc(Integer panelId, Instant from);

    Optional<SolarReading> findByIdAndPanel_Id(Long id, Integer panelId);

    void deleteAllByPanel_Id(Integer panelId);
}
