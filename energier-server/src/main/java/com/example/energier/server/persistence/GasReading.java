package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A single gas usage reading for a meter. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "gas_reading")
public class GasReading {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_gas_reading_seq")
    @SequenceGenerator(name = "energir_gas_reading_seq", sequenceName = "energir_gas_reading_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "meter_id", nullable = false)
    private GasMeter meter;

    @Column(nullable = false)
    private double value;

    @Column(name = "recorded_on", nullable = false)
    private Instant recordedOn;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
