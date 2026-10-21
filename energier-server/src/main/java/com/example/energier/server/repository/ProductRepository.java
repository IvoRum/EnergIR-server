package com.example.energier.server.repository;

import com.example.energier.server.persistence.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findAllByCompany_IdOrderByIdAsc(Integer companyId);

    Optional<Product> findByIdAndCompany_Id(Integer id, Integer companyId);
}
