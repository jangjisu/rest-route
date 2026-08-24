package com.restroute.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_activity_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private AdminActivityLogEntity(String actor, String message, LocalDateTime createdAt) {
        this.actor = actor;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static AdminActivityLogEntity of(String actor, String message, LocalDateTime createdAt) {
        return new AdminActivityLogEntity(actor, message, createdAt);
    }
}
