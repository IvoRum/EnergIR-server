package com.example.energier.server.repository;

import com.example.energier.server.persistence.WaterMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaterMeterRepository extends JpaRepository<WaterMeter, Integer> {
    List<WaterMeter> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<WaterMeter> findByIdAndCompany_Id(Integer id, Integer companyId);
}
