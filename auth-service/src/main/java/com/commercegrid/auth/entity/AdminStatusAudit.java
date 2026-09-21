package com.commercegrid.auth.entity;

import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.enums.StatusReasonCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_status_audit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatusAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long adminId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AdminStatus previousStatus;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AdminStatus newStatus;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusReasonCode reasonCode;

    @Column(nullable = false, length = 500) private String reasonText;
    @Column(nullable = false) private String changedBy;
    @Column(nullable = false) private LocalDateTime changedAt;
}

