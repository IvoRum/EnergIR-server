package com.example.energier.server.repository;

import com.example.energier.server.persistence.ElectricMeterEnergyReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElectricMeterEnergyReadingRepository extends JpaRepository<ElectricMeterEnergyReading, Long> {
}
