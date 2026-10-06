package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "electric_meter", uniqueConstraints = @UniqueConstraint(columnNames = {"edge_id", "edge_meter_id"}))
public class ElectricMeter {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(name = "edge_id", nullable = false)
    private String edgeId;

    @Column(name = "edge_meter_id", nullable = false)
    private Integer edgeMeterId;

    @Column(nullable = false)
    private Integer address;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
