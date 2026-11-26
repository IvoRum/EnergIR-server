package com.example.energier.server.repository;

import com.example.energier.server.persistence.GroupModule;
import com.example.energier.server.persistence.GroupNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupNodeRepository extends JpaRepository<GroupNode, Integer> {
    List<GroupNode> findAllByCompany_IdAndModuleOrderByIdAsc(Integer companyId, GroupModule module);

    Optional<GroupNode> findByIdAndCompany_IdAndModule(Integer id, Integer companyId, GroupModule module);

    List<GroupNode> findAllByParent_Id(Integer parentId);
}
