package com.glowrise.domain;

import com.glowrise.domain.enumerate.ROLE;
import com.glowrise.domain.enumerate.SITE;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AbstractAuditingEntity<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String email;

    private String nickName;

    private String password;

    @Enumerated(EnumType.STRING)
    private ROLE role;

    @Enumerated(EnumType.STRING)
    private SITE site;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Blog blog;

    private String accessToken;

    private String refreshToken;

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

}
