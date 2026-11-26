package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Membership: one object (meter, compressor, product…) in one group. The same
 * object can appear in many groups, which is the point of this table.
 *
 * <p>{@code memberId} points at the module's own table; the module is taken
 * from the group, so the pair is always consistent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(
        name = "group_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "member_id"})
)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_group_member_seq")
    @SequenceGenerator(name = "energir_group_member_seq", sequenceName = "energir_group_member_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupNode group;

    @Column(name = "member_id", nullable = false)
    private Integer memberId;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
