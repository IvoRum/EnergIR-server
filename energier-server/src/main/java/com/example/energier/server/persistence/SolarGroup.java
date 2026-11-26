package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Solar panel group (category). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "solar_group")
public class SolarGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_solar_group_seq")
    @SequenceGenerator(name = "energir_solar_group_seq", sequenceName = "energir_solar_group_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Hex color used by the dashboard, e.g. #f59e0b. */
    private String color;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
