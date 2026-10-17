package com.example.energier.server.repository;

import com.example.energier.server.persistence.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Integer> {
    List<Device> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<Device> findByIdAndCompany_Id(Integer id, Integer companyId);

    Optional<Device> findByCompany_IdAndCode(Integer companyId, String code);

    boolean existsByCompany_IdAndCode(Integer companyId, String code);
}
