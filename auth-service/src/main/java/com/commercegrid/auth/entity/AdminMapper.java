package com.commercegrid.auth.entity;

import com.commercegrid.auth.dto.AdminResponse;
import com.commercegrid.auth.entity.Admin;

public final class AdminMapper {

    private AdminMapper() {}

    public static AdminResponse toAdminResponse(Admin admin) {
        return AdminResponse.builder()
                .adminId(admin.getId())
                .adminName(admin.getName())
                .adminEmail(admin.getEmail())
                .adminRole(admin.getRole())
                .adminStatus(admin.getStatus())
                .createdAt(admin.getCreatedAt())
                .updatedAt(admin.getUpdatedAt())
                .build();
    }
}