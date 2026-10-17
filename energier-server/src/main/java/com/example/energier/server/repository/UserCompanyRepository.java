package com.example.energier.server.repository;

import com.example.energier.server.persistence.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, Integer> {
    List<UserCompany> findByUser_IdOrderByIdAsc(Integer userId);

    Optional<UserCompany> findFirstByUser_IdOrderByIdAsc(Integer userId);

    /** A user belongs to exactly one company. */
    boolean existsByUser_Id(Integer userId);

    boolean existsByUser_IdAndCompany_Id(Integer userId, Integer companyId);
}
