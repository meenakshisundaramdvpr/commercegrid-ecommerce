package com.commercegrid.auth.repository;

import com.commercegrid.auth.entity.Admin;
import com.commercegrid.auth.enums.AdminStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);
    
    long countByStatus(AdminStatus status);
}