package com.example.energier.server.repository;

import com.example.energier.server.persistence.GasMeter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GasMeterRepository extends JpaRepository<GasMeter, Integer> {
    List<GasMeter> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<GasMeter> findByIdAndCompany_Id(Integer id, Integer companyId);
}
