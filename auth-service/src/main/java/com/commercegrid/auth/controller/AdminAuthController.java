package com.commercegrid.auth.controller;

import com.commercegrid.auth.dto.*;
import com.commercegrid.auth.entity.Admin;
import com.commercegrid.auth.service.AdminAuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<AdminResponse> createAdmin(
            @Valid @RequestBody AdminCreateRequest adminCreateRequest) {

        AdminResponse adminResponse =
                adminAuthService.createAdmin(adminCreateRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(adminResponse);
    }
    @PostMapping("/login")
    @Operation(
            summary = "Admin login",
            description = "Authenticates an admin using email and password, "
                    + "and returns an access token on success."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "423", description = "Account locked due to too many failed attempts")
    })
    public ResponseEntity<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest adminLoginRequest) {

        AdminLoginResponse adminLoginResponse =
                adminAuthService.login(adminLoginRequest);

        return ResponseEntity.ok(adminLoginResponse);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Admin> getAdminByEmail(
            @PathVariable String email) {

        return ResponseEntity.ok(
                adminAuthService.getAdminByEmail(email)
        );
    }



    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PatchMapping("/{adminId}/status")
    @Operation(
            summary = "Update admin account status",
            description = "Locks, deactivates, or reactivates an admin account. Requires a mandatory reason for audit purposes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "403", description = "Cannot modify your own status"),
            @ApiResponse(responseCode = "404", description = "Admin not found")
    })



    public ResponseEntity<AdminResponse> updateAdminStatus(
            @PathVariable Long adminId,
            @Valid @RequestBody AdminStatusUpdateRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String requestingAdminEmail = authentication.getName(); // set by JwtAuthFilter -> the verified email

        AdminResponse adminResponse =
                adminAuthService.updateAdminStatus(adminId, request, requestingAdminEmail);

        return ResponseEntity.ok(adminResponse);
    }
}
