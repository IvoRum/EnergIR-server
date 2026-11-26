package com.example.energier.server.repository;

import com.example.energier.server.persistence.Compressor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompressorRepository extends JpaRepository<Compressor, Integer> {
    List<Compressor> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<Compressor> findByIdAndCompany_Id(Integer id, Integer companyId);
}
