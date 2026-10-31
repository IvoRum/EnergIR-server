package com.example.energier.server.repository;

import com.example.energier.server.persistence.GasGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GasGroupRepository extends JpaRepository<GasGroup, Integer> {
    List<GasGroup> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<GasGroup> findByIdAndCompany_Id(Integer id, Integer companyId);
}
