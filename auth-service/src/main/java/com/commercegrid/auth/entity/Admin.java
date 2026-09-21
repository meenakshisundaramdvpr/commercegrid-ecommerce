package com.commercegrid.auth.entity;

import com.commercegrid.auth.enums.AdminRole;
import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.enums.StatusReasonCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "admins",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_admin_email", columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminStatus status;

    @Column
    private String statusReason;

    @Column
    private LocalDateTime statusUpdatedAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @Enumerated(EnumType.STRING)
    private StatusReasonCode statusReasonCode;

}