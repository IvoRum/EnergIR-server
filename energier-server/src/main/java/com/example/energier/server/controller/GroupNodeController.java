package com.example.energier.server.controller;

import com.example.energier.server.model.request.CreateGroupNodeRequest;
import com.example.energier.server.model.request.UpdateGroupNodeRequest;
import com.example.energier.server.model.responce.GroupNodeResponse;
import com.example.energier.server.persistence.GroupModule;
import com.example.energier.server.service.CompanyService;
import com.example.energier.server.service.GroupNodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Group trees shared by every module. {@code module} is part of the path so the
 * client can never mix trees: ELECTRIC, WATER, GAS, PRODUCTION, COMPRESSOR.
 */
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupNodeController {

    private final GroupNodeService groupService;
    private final CompanyService companyService;

    /** Flat list of the module's groups; assemble the tree client-side. */
    @GetMapping
    public ResponseEntity<List<GroupNodeResponse>> list(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(groupService.list(companyId, module));
    }

    @PostMapping
    public ResponseEntity<GroupNodeResponse> create(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @RequestBody CreateGroupNodeRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(companyId, module, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupNodeResponse> update(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @PathVariable Integer id,
            @RequestBody UpdateGroupNodeRequest request
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(groupService.update(companyId, module, id, request));
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<GroupNodeResponse> archive(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(groupService.setArchived(companyId, module, id, true));
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<GroupNodeResponse> unarchive(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(groupService.setArchived(companyId, module, id, false));
    }

    /** Deletes the group; sub-groups move to its parent, members stay untouched. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @PathVariable Integer id
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        groupService.delete(companyId, module, id);
        return ResponseEntity.noContent().build();
    }

    /** Attach a member (meter/compressor/product id) to this group. */
    @PutMapping("/{id}/members/{memberId}")
    public ResponseEntity<GroupNodeResponse> attach(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @PathVariable Integer id,
            @PathVariable Integer memberId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(groupService.attach(companyId, module, id, memberId));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    public ResponseEntity<GroupNodeResponse> detach(
            Authentication authentication,
            @RequestParam(name = "module") GroupModule module,
            @PathVariable Integer id,
            @PathVariable Integer memberId
    ) {
        Integer companyId = companyService.requireCompanyId(authentication.getName());
        return ResponseEntity.ok(groupService.detach(companyId, module, id, memberId));
    }
}
