package com.example.energier.server.repository;

import com.example.energier.server.persistence.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {
    List<ProductionRecord> findByProduct_IdOrderByRecordedOnDesc(Integer productId);

    Optional<ProductionRecord> findByIdAndProduct_Id(Long id, Integer productId);

    void deleteAllByProduct_Id(Integer productId);
}
