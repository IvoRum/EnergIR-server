package com.example.energier.server.repository;

import com.example.energier.server.persistence.WaterGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaterGroupRepository extends JpaRepository<WaterGroup, Integer> {
    List<WaterGroup> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<WaterGroup> findByIdAndCompany_Id(Integer id, Integer companyId);
}
