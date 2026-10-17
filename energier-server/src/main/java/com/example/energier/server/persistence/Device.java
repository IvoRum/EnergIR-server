package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * An edge device installed in the factory. It reads the electric meters that
 * share its {@link #code} as their `edgeId`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(
        name = "device",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "code"})
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_device_seq")
    @SequenceGenerator(name = "energir_device_seq", sequenceName = "energir_device_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Edge identifier shared with ElectricMeter.edgeId, e.g. "edge-01". */
    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    private String location;

    /** Hardware/model family, e.g. "edge-v1". */
    private String type;

    @Column(name = "communication_type")
    private String communicationType;

    private String port;

    private String baudrate;

    private String parity;

    @Column(name = "data_bits")
    private String dataBits;

    /** online | offline | warning */
    @Column(nullable = false)
    private String status;

    @Column(name = "last_seen")
    private Instant lastSeen;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
