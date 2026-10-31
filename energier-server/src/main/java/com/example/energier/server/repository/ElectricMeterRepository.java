package com.example.energier.server.repository;

import com.example.energier.server.persistence.ElectricMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElectricMeterRepository extends JpaRepository<ElectricMeter, Integer> {

    /** Edge ingestion looks a meter up without a company (edge ids are unique). */
    Optional<ElectricMeter> findByEdgeIdAndEdgeMeterId(String edgeId, Integer edgeMeterId);

    List<ElectricMeter> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<ElectricMeter> findByIdAndCompany_Id(Integer id, Integer companyId);

    Optional<ElectricMeter> findByCompany_IdAndEdgeIdAndEdgeMeterId(
            Integer companyId, String edgeId, Integer edgeMeterId);
}
