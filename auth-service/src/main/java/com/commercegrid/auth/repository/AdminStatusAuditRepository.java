package com.commercegrid.auth.repository;

import com.commercegrid.auth.entity.AdminStatusAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminStatusAuditRepository extends JpaRepository<AdminStatusAudit, Long> {
    List<AdminStatusAudit> findByAdminIdOrderByChangedAtDesc(Long adminId);
}