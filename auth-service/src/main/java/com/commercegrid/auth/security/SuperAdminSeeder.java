package com.commercegrid.auth.security;

import com.commercegrid.auth.entity.Admin;
import com.commercegrid.auth.enums.AdminRole;
import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuperAdminSeeder implements CommandLineRunner {

    private final AdminRepository repo;
    private final PasswordEncoder encoder;

    @Value("${bootstrap.super-admin.email}")
    private String email;

    @Value("${bootstrap.super-admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (repo.existsByEmail(email)) return;

        repo.save(Admin.builder()
                .name("Super Admin")
                .email(email)
                .password(encoder.encode(password))
                .role(AdminRole.SUPER_ADMIN)
                .status(AdminStatus.ACTIVE)
                .build());
    }
}