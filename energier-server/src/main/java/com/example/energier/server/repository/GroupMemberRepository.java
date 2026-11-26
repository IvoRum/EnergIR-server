package com.example.energier.server.repository;

import com.example.energier.server.persistence.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findAllByGroup_Id(Integer groupId);

    List<GroupMember> findAllByMemberId(Integer memberId);

    Optional<GroupMember> findByGroup_IdAndMemberId(Integer groupId, Integer memberId);

    void deleteAllByGroup_Id(Integer groupId);
}
