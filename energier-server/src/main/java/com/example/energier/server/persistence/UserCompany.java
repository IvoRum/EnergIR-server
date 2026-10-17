package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Membership link between a user and a company (many-to-many). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "user_company",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "company_id"})
)
public class UserCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_user_company_seq")
    @SequenceGenerator(name = "energir_user_company_seq", sequenceName = "energir_user_company_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
