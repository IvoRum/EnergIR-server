package com.example.energier.server.model.responce;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One node of a module's group tree. The list endpoint returns every node flat
 * (with {@code parentId}), so the client can build the tree without ordering
 * assumptions; {@code memberIds} is the many-to-many membership of this group.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupNodeResponse {
    private Integer id;
    private String module;
    private String name;
    private String description;
    private String color;
    /** null for a root group. */
    private Integer parentId;
    private boolean archived;
    private String createdOn;
    private List<Integer> memberIds;
}
