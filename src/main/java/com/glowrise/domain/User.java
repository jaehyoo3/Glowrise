package com.glowrise.domain;

import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name= "User")
@NoArgsConstructor
public class User extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    private String nickName;

    private String password;

    private ROLE role;

    private SITE site;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Blog blog;

}
