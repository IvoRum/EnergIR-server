package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * A tenant/workspace a user belongs to. A user can belong to many companies;
 * the client selects one after login and stores its code for the session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company", uniqueConstraints = @UniqueConstraint(columnNames = {"code"}))
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_company_seq")
    @SequenceGenerator(name = "energir_company_seq", sequenceName = "energir_company_seq", allocationSize = 1)
    private Integer id;

    @Column(nullable = false)
    private String name;

    /** Short, unique, human-readable code stored in the user's session. */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
