package com.example.energier.server.repository;

import com.example.energier.server.persistence.SolarGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolarGroupRepository extends JpaRepository<SolarGroup, Integer> {
    List<SolarGroup> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<SolarGroup> findByIdAndCompany_Id(Integer id, Integer companyId);
}
