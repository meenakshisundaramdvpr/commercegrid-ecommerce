package com.commercegrid.auth.service;

import com.commercegrid.auth.config.JwtUtil;
import com.commercegrid.auth.dto.*;
import com.commercegrid.auth.entity.Admin;
import com.commercegrid.auth.entity.AdminMapper;
import com.commercegrid.auth.entity.AdminStatusAudit;
import com.commercegrid.auth.enums.AdminRole;
import com.commercegrid.auth.enums.AdminStatus;
import com.commercegrid.auth.exception.*;
import com.commercegrid.auth.repository.AdminRepository;

import com.commercegrid.auth.repository.AdminStatusAuditRepository;
import com.commercegrid.auth.util.AuthConstants;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j                      // add this

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AdminStatusAuditRepository adminStatusAuditRepository;

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

        if (admin.getStatus() == AdminStatus.LOCKED) {
            throw new AccountLockedException("Your account is locked. Contact your security administrator");
        }
        if (admin.getStatus() == AdminStatus.INACTIVE) {
            throw new InvalidAdminOperationException("Your account is deactivated. Contact your administrator");
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
    @Transactional
    public AdminResponse updateAdminStatus(
            Long adminId, AdminStatusUpdateRequest request, String requestingAdminEmail) {

        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found with id: " + adminId));

        AdminStatus previous = admin.getStatus();
        AdminStatus target = request.getNewStatus();

        if (admin.getEmail().equalsIgnoreCase(requestingAdminEmail)) {
            throw new InvalidAdminOperationException("You cannot change your own account status");
        }
        if (previous == target) {
            throw new InvalidAdminOperationException("Admin is already " + target);
        }
        if (request.getReasonCode().getTargetStatus() != target) {
            throw new InvalidAdminOperationException(
                    "Reason code " + request.getReasonCode() + " is not valid for status " + target);
        }
        if (previous == AdminStatus.INACTIVE && target == AdminStatus.LOCKED) {
            throw new InvalidAdminOperationException("Reactivate the account before locking it");
        }
        // Never leave the system without an active admin
        if (previous == AdminStatus.ACTIVE && target != AdminStatus.ACTIVE
                && adminRepository.countByStatus(AdminStatus.ACTIVE) <= 1) {
            throw new InvalidAdminOperationException("Cannot disable the last active admin");
        }

        admin.setStatus(target);
        admin.setStatusReasonCode(request.getReasonCode());
        admin.setStatusReason(request.getStatusReason());
        admin.setStatusUpdatedAt(LocalDateTime.now());

        if (target == AdminStatus.ACTIVE) {          // clean slate on reactivation
            admin.setFailedAttempts(0);
            admin.setLockedUntil(null);
        }

        Admin saved = adminRepository.save(admin);

        adminStatusAuditRepository.save(AdminStatusAudit.builder()
                .adminId(saved.getId())
                .previousStatus(previous)
                .newStatus(target)
                .reasonCode(request.getReasonCode())
                .reasonText(request.getStatusReason())
                .changedBy(requestingAdminEmail)
                .changedAt(LocalDateTime.now())
                .build());

        log.info("Admin {} status {} -> {} by {} ({})",
                saved.getId(), previous, target, requestingAdminEmail, request.getReasonCode());

        if (target == AdminStatus.LOCKED || target == AdminStatus.INACTIVE) {
            try {
                emailService.sendStatusChangeEmail(
                        saved.getEmail(), saved.getName(), target, request.getStatusReason());
            } catch (Exception ex) {
                log.error("Status change email failed for admin {}", saved.getId(), ex);
            }
        }
        return AdminMapper.toAdminResponse(saved);
    }
}