package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A compressor whose air output is tracked in cubic metres. Deliberately small:
 * name, description, and the readings (quantity + date). No pressure, no
 * location.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "compressor")
public class Compressor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_compressor_seq")
    @SequenceGenerator(name = "energir_compressor_seq", sequenceName = "energir_compressor_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /**
     * Optional link to the electric meter that powers this compressor, so the
     * energy behind the compressed air can be costed. Null when the compressor
     * has no meter of its own.
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "electric_meter_id")
    private ElectricMeter electricMeter;

    /** Soft delete: archived compressors keep their readings but leave the list. */
    @Column(name = "archived")
    private Boolean archived;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
