package com.glowrise.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table
@Getter
public class Blog extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String title;

    private String description;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Override
    public Long getId() {
        return this.id;
    }
}
