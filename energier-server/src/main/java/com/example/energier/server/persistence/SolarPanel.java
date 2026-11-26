package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/** A solar panel installation owned by a company. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "solar_panel")
public class SolarPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_solar_panel_seq")
    @SequenceGenerator(name = "energir_solar_panel_seq", sequenceName = "energir_solar_panel_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private SolarGroup group;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String location;

    /** kW | MW | W */
    @Column(nullable = false)
    private String unit;

    /** generating | low_output | offline */
    @Column(nullable = false)
    private String status;

    private String manufacturer;

    @Column(name = "install_date")
    private LocalDate installDate;

    /** Maximum rated output in the panel's unit. */
    @Column(nullable = false)
    private double capacity;

    /** Percentage of rated capacity currently being produced. */
    @Column(nullable = false)
    private double efficiency;

    /** Latest reported power output. */
    @Column(name = "current_output", nullable = false)
    private double currentOutput;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /** Soft delete: archived panels keep their readings but leave the active list. */
    @Column(name = "archived")
    private Boolean archived;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
