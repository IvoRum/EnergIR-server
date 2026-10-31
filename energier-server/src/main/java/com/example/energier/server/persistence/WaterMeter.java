package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/** A water meter owned by a company. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "water_meter")
public class WaterMeter {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_water_meter_seq")
    @SequenceGenerator(name = "energir_water_meter_seq", sequenceName = "energir_water_meter_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private WaterGroup group;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    private String location;

    /** liters | gallons | cubic_meters | milliliters */
    @Column(nullable = false)
    private String unit;

    /** active | warning | offline */
    @Column(nullable = false)
    private String status;

    private String manufacturer;

    @Column(name = "install_date")
    private LocalDate installDate;

    /** Running total of reported usage. */
    @Column(name = "current_value", nullable = false)
    private double currentValue;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
