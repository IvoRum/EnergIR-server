package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A single power-output reading for a solar panel. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "solar_reading")
public class SolarReading {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_solar_reading_seq")
    @SequenceGenerator(name = "energir_solar_reading_seq", sequenceName = "energir_solar_reading_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "panel_id", nullable = false)
    private SolarPanel panel;

    /** Power produced at the recorded time, in the panel's unit. */
    @Column(name = "power_output", nullable = false)
    private double powerOutput;

    /** Irradiance in W/m², when the device reports it. */
    private Double irradiance;

    @Column(name = "recorded_on", nullable = false)
    private Instant recordedOn;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
