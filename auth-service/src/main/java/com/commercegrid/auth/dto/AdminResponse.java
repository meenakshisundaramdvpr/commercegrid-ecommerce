package com.commercegrid.auth.dto;

import com.commercegrid.auth.enums.AdminRole;
import com.commercegrid.auth.enums.AdminStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminResponse {

    private Long adminId;
    private String adminName;
    private String adminEmail;
    private AdminRole adminRole;
    private AdminStatus adminStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}