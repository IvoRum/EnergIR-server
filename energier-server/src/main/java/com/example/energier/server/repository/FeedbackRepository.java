package com.example.energier.server.repository;

import com.example.energier.server.persistence.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Integer> {
    List<Feedback> findAllByCompany_IdOrderByCreatedOnDesc(Integer companyId);
}
