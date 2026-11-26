package com.example.energier.server.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Partial update. {@code parentId} semantics: omitted/null keeps the current
 * parent, {@code 0} moves the group to the root, {@code >0} re-parents it.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGroupNodeRequest {
    private String name;
    private String description;
    private String color;
    private Integer parentId;
}
