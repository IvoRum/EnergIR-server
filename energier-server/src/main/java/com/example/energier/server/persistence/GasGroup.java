package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Gas meter group (category). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "gas_group")
public class GasGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_gas_group_seq")
    @SequenceGenerator(name = "energir_gas_group_seq", sequenceName = "energir_gas_group_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Hex color used by the dashboard, e.g. #f97316. */
    private String color;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
