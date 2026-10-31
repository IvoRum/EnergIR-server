package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A production entry (quantity produced at a point in time). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "production_record")
public class ProductionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_production_record_seq")
    @SequenceGenerator(name = "energir_production_record_seq", sequenceName = "energir_production_record_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private double quantity;

    private String quality;

    @Column(name = "recorded_on", nullable = false)
    private Instant recordedOn;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
