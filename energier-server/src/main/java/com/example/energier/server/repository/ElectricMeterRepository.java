package com.example.energier.server.repository;

import com.example.energier.server.persistence.ElectricMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElectricMeterRepository extends JpaRepository<ElectricMeter, Integer> {
    Optional<ElectricMeter> findByEdgeIdAndEdgeMeterId(String edgeId, Integer edgeMeterId);
}
