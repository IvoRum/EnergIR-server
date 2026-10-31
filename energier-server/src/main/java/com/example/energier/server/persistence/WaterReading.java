package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A single water usage reading for a meter. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "water_reading")
public class WaterReading {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_water_reading_seq")
    @SequenceGenerator(name = "energir_water_reading_seq", sequenceName = "energir_water_reading_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private WaterMeter meter;

    @Column(nullable = false)
    private double value;

    @Column(name = "recorded_on", nullable = false)
    private Instant recordedOn;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
