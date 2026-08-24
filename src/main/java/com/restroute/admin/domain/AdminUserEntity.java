package com.restroute.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;

    private AdminUserEntity(String username, String password, AdminRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public static AdminUserEntity of(String username, String password, AdminRole role) {
        return new AdminUserEntity(username, password, role);
    }
}
