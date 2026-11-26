package com.example.energier.server.repository;

import com.example.energier.server.persistence.CompressorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompressorReadingRepository extends JpaRepository<CompressorReading, Long> {
    List<CompressorReading> findByCompressor_IdOrderByRecordedOnDesc(Integer compressorId);

    List<CompressorReading> findByCompressor_IdOrderByRecordedOnAsc(Integer compressorId);

    Optional<CompressorReading> findByIdAndCompressor_Id(Long id, Integer compressorId);

    void deleteAllByCompressor_Id(Integer compressorId);
}
