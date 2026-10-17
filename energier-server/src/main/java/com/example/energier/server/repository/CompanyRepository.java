package com.example.energier.server.repository;

import com.example.energier.server.persistence.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    Optional<Company> findByCode(String code);

    boolean existsByCode(String code);
}
