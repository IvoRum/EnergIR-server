package com.example.energier.server.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** User feedback (bug report / feature request / improvement / other). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Setter
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "energir_feedback_seq")
    @SequenceGenerator(name = "energir_feedback_seq", sequenceName = "energir_feedback_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "user_email")
    private String userEmail;

    /** bug | feature | improvement | other */
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    /** Optional contact email typed by the user. */
    private String email;

    /** 1..5, optional. */
    private Integer rating;

    /** Client clock (epoch millis) at submission time. */
    @Column(name = "client_timestamp")
    private Long clientTimestamp;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(length = 1000)
    private String url;

    @Column(name = "created_on", nullable = false)
    private Instant createdOn;
}
