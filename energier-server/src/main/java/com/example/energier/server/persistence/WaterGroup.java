package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Water meter group (category). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "water_group")
public class WaterGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_water_group_seq")
    @SequenceGenerator(name = "energir_water_group_seq", sequenceName = "energir_water_group_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    private String description;

    /** Hex color used by the dashboard, e.g. #0ea5e9. */
    private String color;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
