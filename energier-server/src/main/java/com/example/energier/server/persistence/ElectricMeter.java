package com.example.energier.server.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @Entity @Table(name = "electric_meter")
public class ElectricMeter {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "voltagell1", nullable = false, precision = 7, scale = 15)
    private BigDecimal voltagell1;

    @Column(name = "voltagell2", nullable = false, precision = 7, scale = 15)
    private BigDecimal voltagell2;

    @Column(name = "voltagell3", nullable = false, precision = 7, scale = 15)
    private BigDecimal voltagell3;

    @Column(name = "currentl1", nullable = false, precision = 7, scale = 15)
    private BigDecimal currentl1;

    @Column(name = "currentl2", nullable = false, precision = 7, scale = 15)
    private BigDecimal currentl2;

    @Column(name = "currentl3", nullable = false, precision = 7, scale = 15)
    private BigDecimal currentl3;

    @Column(name = "activepowerl1", nullable = false, precision = 7, scale = 15)
    private BigDecimal activepowerl1;

    @Column(name = "activepowerl2", nullable = false, precision = 7, scale = 15)
    private BigDecimal activepowerl2;

    @Column(name = "activepowerl3", nullable = false, precision = 7, scale = 15)
    private BigDecimal activepowerl3;

    @Column(name = "pfl1", nullable = false, precision = 7, scale = 15)
    private BigDecimal pfl1;

    @Column(name = "pfl2", nullable = false, precision = 7, scale = 15)
    private BigDecimal pfl2;

    @Column(name = "pfl3", nullable = false, precision = 7, scale = 15)
    private BigDecimal pfl3;

    @Column(name = "pf", nullable = false, precision = 7, scale = 15)
    private BigDecimal pf;

    @Column(name = "totalactiveenergyimporttariff1", nullable = false, precision = 7, scale = 15)
    private BigDecimal totalactiveenergyimporttariff1;

}