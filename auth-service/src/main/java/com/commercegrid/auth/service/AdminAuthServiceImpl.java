package com.commercegrid.auth.service;

import com.commercegrid.auth.config.JwtUtil;
import com.commercegrid.auth.dto.*;
import com.commercegrid.auth.entity.Admin;
import com.commercegrid.auth.entity.AdminMapper;
import com.commercegrid.auth.enums.AdminRole;
import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.exception.*;
import com.commercegrid.auth.repository.AdminRepository;

import com.commercegrid.auth.util.AuthConstants;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

   /* @Override
    public AdminResponse createAdmin(AdminCreateRequest adminCreateRequest) {

        if (adminRepository.existsByEmail(adminCreateRequest.getAdminEmail())) {
            throw new DuplicateAdminException(
                    "Admin already exists with email: " + adminCreateRequest.getAdminEmail()
            );
        }

        Admin admin = Admin.builder()
                .name(adminCreateRequest.getAdminName())
                .email(adminCreateRequest.getAdminEmail())
                .password(passwordEncoder.encode(adminCreateRequest.getAdminPassword()))
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .build();

        Admin savedAdmin = adminRepository.save(admin);

        return AdminMapper.toAdminResponse(savedAdmin);
    }
*/
    @Override
    public Admin getAdminByEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() ->
                        new AdminNotFoundException(
                                "Admin not found with email: " + email
                        )
                );
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest adminLoginRequest) {

        Admin admin = adminRepository.findByEmail(adminLoginRequest.getAdminEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("No admin found with this email")
                );

        // 1. Check if currently locked
        if (admin.getLockedUntil() != null) {
            if (admin.getLockedUntil().isAfter(LocalDateTime.now())) {
                long secondsLeft = Duration.between(
                        LocalDateTime.now(), admin.getLockedUntil()
                ).getSeconds();

                throw new AccountLockedException(
                        "Account locked due to multiple failed attempts. Try again in "
                                + secondsLeft + " seconds"
                );
            } else {
                // lock window expired -> reset
                admin.setFailedAttempts(0);
                admin.setLockedUntil(null);
            }
        }

        // 2. Check password
        boolean passwordMatches = passwordEncoder.matches(
                adminLoginRequest.getAdminPassword(),
                admin.getPassword()
        );

        if (!passwordMatches) {
            int attempts = admin.getFailedAttempts() + 1;
            admin.setFailedAttempts(attempts);

            if (attempts >= AuthConstants.MAX_LOGIN_ATTEMPTS) {
                admin.setLockedUntil(
                        LocalDateTime.now().plusMinutes(AuthConstants.ACCOUNT_LOCK_DURATION_MINUTES)
                );
                adminRepository.save(admin);

                throw new AccountLockedException(
                        "Too many failed attempts. Account locked for "
                                + AuthConstants.ACCOUNT_LOCK_DURATION_MINUTES + " minutes"
                );
            }

            adminRepository.save(admin);

            int remaining = AuthConstants.MAX_LOGIN_ATTEMPTS - attempts;
            throw new InvalidCredentialsException(
                    "Incorrect password. " + remaining + " attempt(s) remaining"
            );
        }

        // 3. Success -> reset counters
        if (admin.getFailedAttempts() != 0 || admin.getLockedUntil() != null) {
            admin.setFailedAttempts(0);
            admin.setLockedUntil(null);
            adminRepository.save(admin);
        }

        if (admin.getStatus() != AdminStatus.ACTIVE) {
            throw new InvalidCredentialsException(
                    "Admin account is " + admin.getStatus().name().toLowerCase()
            );
        }

        String accessToken = jwtUtil.generateToken(admin.getEmail(), admin.getRole().name());

        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .admin(AdminMapper.toAdminResponse(admin))
                .build();
    }

    private final EmailService emailService; // add to fields

    @Override
    public AdminResponse createAdmin(AdminCreateRequest adminCreateRequest) {

        if (adminRepository.existsByEmail(adminCreateRequest.getAdminEmail())) {
            throw new DuplicateAdminException(
                    "Admin already exists with email: " + adminCreateRequest.getAdminEmail()
            );
        }

        Admin admin = Admin.builder()
                .name(adminCreateRequest.getAdminName())
                .email(adminCreateRequest.getAdminEmail())
                .password(passwordEncoder.encode(adminCreateRequest.getAdminPassword()))
                .role(AdminRole.ADMIN)
                .status(AdminStatus.ACTIVE)
                .build();

        Admin savedAdmin = adminRepository.save(admin);

        // Send welcome email -- failure here should not roll back admin creation
        try {
            emailService.sendAdminWelcomeEmail(
                    savedAdmin.getEmail(),
                    savedAdmin.getName(),
                    adminCreateRequest.getAdminPassword() // plaintext, only available here, pre-encoding
            );
        } catch (Exception ex) {
            // Log this properly in a real system (SLF4J), don't let email failure break account creation
            System.err.println("Failed to send welcome email to " + savedAdmin.getEmail() + ": " + ex.getMessage());
        }

        return AdminMapper.toAdminResponse(savedAdmin);
    }
    @Override
    public AdminResponse updateAdminStatus(
            Long adminId, AdminStatusUpdateRequest request, String requestingAdminEmail) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found with id: " + adminId));

        // Four-eyes principle: an admin can't change their own status
        if (admin.getEmail().equalsIgnoreCase(requestingAdminEmail)) {
            throw new InvalidAdminOperationException("You cannot change your own account status");
        }

        AdminStatus previousStatus = admin.getStatus();

        admin.setStatus(request.getNewStatus());
        admin.setStatusReason(request.getStatusReason());
        admin.setStatusUpdatedAt(LocalDateTime.now());

        Admin updatedAdmin = adminRepository.save(admin);

        // Notify only on an actual change to LOCKED/INACTIVE -- not on reactivation or no-op
        if (previousStatus != request.getNewStatus()
                && (request.getNewStatus() == AdminStatus.LOCKED
                || request.getNewStatus() == AdminStatus.INACTIVE)) {
            try {
                emailService.sendStatusChangeEmail(
                        updatedAdmin.getEmail(),
                        updatedAdmin.getName(),
                        updatedAdmin.getStatus(),
                        request.getStatusReason()
                );
            } catch (Exception ex) {
                System.err.println("Failed to send status change email: " + ex.getMessage());
            }
        }

        return AdminMapper.toAdminResponse(updatedAdmin);
    }
}