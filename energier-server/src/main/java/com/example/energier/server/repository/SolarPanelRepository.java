package com.example.energier.server.repository;

import com.example.energier.server.persistence.SolarPanel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolarPanelRepository extends JpaRepository<SolarPanel, Integer> {
    List<SolarPanel> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<SolarPanel> findByIdAndCompany_Id(Integer id, Integer companyId);
}
