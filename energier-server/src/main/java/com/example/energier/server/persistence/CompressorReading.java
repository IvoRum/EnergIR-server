package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** One production record for a compressor: cubic metres on a given moment. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "compressor_reading")
public class CompressorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_compressor_reading_seq")
    @SequenceGenerator(name = "energir_compressor_reading_seq", sequenceName = "energir_compressor_reading_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "compressor_id", nullable = false)
    private Compressor compressor;

    /** Produced air, in cubic metres. */
    @Column(nullable = false)
    private double quantity;

    @Column(name = "recorded_on", nullable = false)
    private Instant recordedOn;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
