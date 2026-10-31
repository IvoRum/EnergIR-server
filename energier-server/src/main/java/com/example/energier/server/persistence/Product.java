package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** A produced product / production line item. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_product_seq")
    @SequenceGenerator(name = "energir_product_seq", sequenceName = "energir_product_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ProductGroup group;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    /** pieces | kilograms | liters | meters | tons */
    @Column(nullable = false)
    private String unit;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
