package com.example.energier.server.service;

import com.example.energier.server.model.request.CreateGroupNodeRequest;
import com.example.energier.server.model.request.UpdateGroupNodeRequest;
import com.example.energier.server.model.responce.GroupNodeResponse;
import com.example.energier.server.persistence.Company;
import com.example.energier.server.persistence.GroupMember;
import com.example.energier.server.persistence.GroupModule;
import com.example.energier.server.persistence.GroupNode;
import com.example.energier.server.repository.CompanyRepository;
import com.example.energier.server.repository.GroupMemberRepository;
import com.example.energier.server.repository.GroupNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Group trees for every module: a group holds members (meters, compressors,
 * products…) and other groups, and a member can be in many groups.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class GroupNodeService {

    private final GroupNodeRepository nodeRepository;
    private final GroupMemberRepository memberRepository;
    private final CompanyRepository companyRepository;

    /** Every node of one module, flat; the client assembles the tree. */
    public List<GroupNodeResponse> list(Integer companyId, GroupModule module) {
        return nodeRepository.findAllByCompany_IdAndModuleOrderByIdAsc(companyId, module).stream()
                .map(this::toResponse)
                .toList();
    }

    public GroupNodeResponse create(Integer companyId, GroupModule module, CreateGroupNodeRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        GroupNode parent = request.getParentId() == null || request.getParentId() <= 0
                ? null
                : find(companyId, module, request.getParentId());

        GroupNode node = GroupNode.builder()
                .company(company)
                .module(module)
                .name(requireText(request.getName(), "Group name"))
                .description(trimToNull(request.getDescription()))
                .color(trimToNull(request.getColor()))
                .parent(parent)
                .createdOn(Instant.now())
                .build();

        return toResponse(nodeRepository.save(node));
    }

    public GroupNodeResponse update(Integer companyId, GroupModule module, Integer id, UpdateGroupNodeRequest request) {
        GroupNode node = find(companyId, module, id);

        if (request.getName() != null) node.setName(requireText(request.getName(), "Group name"));
        if (request.getDescription() != null) node.setDescription(trimToNull(request.getDescription()));
        if (request.getColor() != null) node.setColor(trimToNull(request.getColor()));

        if (request.getParentId() != null) {
            if (request.getParentId() == 0) {
                node.setParent(null);
            } else {
                if (request.getParentId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A group cannot be its own parent");
                }
                GroupNode parent = find(companyId, module, request.getParentId());
                assertNotDescendant(id, parent);
                node.setParent(parent);
            }
        }

        return toResponse(nodeRepository.save(node));
    }

    public GroupNodeResponse setArchived(Integer companyId, GroupModule module, Integer id, boolean archived) {
        GroupNode node = find(companyId, module, id);
        node.setArchived(archived);
        return toResponse(nodeRepository.save(node));
    }

    /**
     * Deletes a group. Its sub-groups are re-parented to its own parent (so no
     * subtree is lost) and its memberships are removed. Members themselves are
     * never deleted.
     */
    public void delete(Integer companyId, GroupModule module, Integer id) {
        GroupNode node = find(companyId, module, id);

        for (GroupNode child : nodeRepository.findAllByParent_Id(id)) {
            child.setParent(node.getParent());
            nodeRepository.save(child);
        }
        memberRepository.deleteAllByGroup_Id(id);
        nodeRepository.delete(node);
    }

    /** Adds a member to a group. Idempotent: adding twice is a no-op. */
    public GroupNodeResponse attach(Integer companyId, GroupModule module, Integer id, Integer memberId) {
        GroupNode node = find(companyId, module, id);
        if (memberId == null || memberId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "memberId is required");
        }
        if (memberRepository.findByGroup_IdAndMemberId(id, memberId).isEmpty()) {
            memberRepository.save(GroupMember.builder()
                    .group(node)
                    .memberId(memberId)
                    .createdOn(Instant.now())
                    .build());
        }
        return toResponse(node);
    }

    public GroupNodeResponse detach(Integer companyId, GroupModule module, Integer id, Integer memberId) {
        GroupNode node = find(companyId, module, id);
        memberRepository.findByGroup_IdAndMemberId(id, memberId)
                .ifPresent(memberRepository::delete);
        return toResponse(node);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private GroupNode find(Integer companyId, GroupModule module, Integer id) {
        return nodeRepository.findByIdAndCompany_IdAndModule(id, companyId, module)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Group with id " + id + " not found"));
    }

    /** Walks up from {@code candidate}; rejects if {@code id} is one of its ancestors. */
    private void assertNotDescendant(Integer id, GroupNode candidate) {
        GroupNode cursor = candidate;
        int guard = 0;
        while (cursor != null) {
            if (cursor.getId().equals(id)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Cannot move a group inside its own sub-group");
            }
            cursor = cursor.getParent();
            if (++guard > 100) break;
        }
    }

    private String requireText(String value, String field) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private GroupNodeResponse toResponse(GroupNode node) {
        return GroupNodeResponse.builder()
                .id(node.getId())
                .module(node.getModule().name())
                .name(node.getName())
                .description(node.getDescription())
                .color(node.getColor())
                .parentId(node.getParent() != null ? node.getParent().getId() : null)
                .archived(Boolean.TRUE.equals(node.getArchived()))
                .createdOn(node.getCreatedOn().atZone(ZoneId.systemDefault()).toLocalDate().toString())
                .memberIds(memberRepository.findAllByGroup_Id(node.getId()).stream()
                        .map(GroupMember::getMemberId)
                        .toList())
                .build();
    }
}
