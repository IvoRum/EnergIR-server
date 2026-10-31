package com.example.energier.server.repository;

import com.example.energier.server.persistence.ProductGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductGroupRepository extends JpaRepository<ProductGroup, Integer> {
    List<ProductGroup> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<ProductGroup> findByIdAndCompany_Id(Integer id, Integer companyId);
}
