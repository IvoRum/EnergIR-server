package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A node in a module's group tree. A group can hold both members (meters,
 * compressors, products…) and other groups, via {@code parent}.
 *
 * <p>One table serves every module: {@link #module} says which. This keeps the
 * hierarchy logic in one place instead of five near-identical copies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(
        name = "group_node",
        indexes = @Index(name = "idx_group_node_lookup", columnList = "company_id, module")
)
public class GroupNode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_group_node_seq")
    @SequenceGenerator(name = "energir_group_node_seq", sequenceName = "energir_group_node_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GroupModule module;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 32)
    private String color;

    /** Parent group; null for a root group. */
    @ManyToOne(optional = true)
    @JoinColumn(name = "parent_id")
    private GroupNode parent;

    @Column(name = "archived")
    private Boolean archived;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
